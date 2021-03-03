package no.unit.alma;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import no.unit.marc.Reference;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;


public class UpdateAlmaDescriptionHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {

    public static final String QUERY_STRING_PARAMETERS_KEY = "queryStringParameters";
    public static final String ISBN_KEY = "isbn";
    public static final String DESCRPTION_KEY = "description";
    public static final String URL_KEY = "url";

    public static final String MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS =
            "Missing event element 'queryStringParameters'.";
    public static final String MANDATORY_PARAMETER_MISSING =
            "Mandatory parameter 'isbn', 'description' or 'url' is missing.";


    protected final transient GetRecordByIsbnConnection connection = new GetRecordByIsbnConnection();

    /**
     * Main lambda function to update the links in Alma records.
     * Program flow:
     * 1. Get a REFERENCE LIST from alma-sru through a lambda.
     * 2. Loop through the LIST (and do the following for every OBJECT).
     * 3. Get the MMS_ID from the REFERENCE OBJECT.
     * 4. Use the MMS_ID to get a BIB-RECORD from the alma-api.
     * 5. Insert the new link-data into the BIB-RECORD.
     * 6. Push the updated BIB-RECORD back to the alma through a put-request to the api.
     * @param input payload with identifying parameters
     * @return a GatewayResponse
     */
    @Override
    @SuppressWarnings("unchecked")
    public GatewayResponse handleRequest(final Map<String, Object> input, Context context) {
        //TODO: Remove - For testing only
        System.out.println(input);

        GatewayResponse gatewayResponse = new GatewayResponse();
        AlmaConnection almaConnection = new AlmaConnection();
        XmlParser xmlParser = new XmlParser();
        Boolean othersSucceeded = false;
        Boolean othersFailed = false;
        Map<String, String> inputParameters;
        String secretKey;
        List<Reference> referenceList;
        String gatewayResponseBody = "";
        gatewayResponse.setStatusCode(400);

        try {
            Config.getInstance().checkProperties();
            inputParameters = this.checkParameters(input);
        } catch (ParameterException e) {
            DebugUtils.dumpException(e);
            gatewayResponse.setErrorBody(e.getMessage()); // Exception contains missing parameter name
            gatewayResponse.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
            return gatewayResponse;
        }

        try {
            SecretRetriever secretRetriever = new SecretRetriever();
            secretKey = secretRetriever.getSecret();
        } catch (Exception e) {
            gatewayResponse.setStatusCode(401);
            gatewayResponse.setErrorBody("Couldn't retrieve the API-key " + e.getMessage());
            return gatewayResponse;
        }

        try {
            /* Step 1. Get a REFERENCE LIST from alma-sru through a lambda. */
            String isbn = inputParameters.get(ISBN_KEY);
            referenceList = getReferenceListByIsbn(isbn);
            if (referenceList == null) {
                gatewayResponse.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
                gatewayResponse.setErrorBody("No reference object retrieved for this ISBN");
                return gatewayResponse;
            }

            gatewayResponseBody += "Got " + referenceList.size() + " reference object(s) from alma-sru \n";

            /* 2. Loop through the LIST. */
            for (Reference reference : referenceList) {
                /* 3. Get the MMS_ID from the REFERENCE OBJECT. */
                String mmsId = reference.getId();
                /* 4. Use the MMS_ID to get a BIB-RECORD from the alma-api. */
                HttpResponse<String> almaResponse = almaConnection.sendGet(mmsId, secretKey);
                if (almaResponse.statusCode() == 200) {
                    gatewayResponseBody += "Got the BIB-post for: " + mmsId + "\n";
                } else {
                    gatewayResponseBody +=
                            "Couldn't get the BIB-post for: " + mmsId + ". Alma responded with statuscode: "
                            + almaResponse.statusCode() + "\n";
                    if (othersSucceeded) {
                        gatewayResponse.setStatusCode(207);
                    } else {
                        gatewayResponse.setStatusCode(400);
                    }
                    othersFailed = true;
                    continue;
                }

                String bibRecordXml = almaResponse.body();

                /* 5. Insert the new link-data into the BIB-RECORD. */
                if (xmlParser.alreadyExists(inputParameters.get(DESCRPTION_KEY),
                        inputParameters.get(URL_KEY), bibRecordXml)) {
                    gatewayResponseBody += "409 The BIB-post with mms_id: " + mmsId + " is already up to date. \n";
                    if (othersSucceeded) {
                        gatewayResponse.setStatusCode(207);
                    } else {
                        gatewayResponse.setStatusCode(409);
                    }
                    othersFailed = true;
                    continue;
                }

                Document updateNode = xmlParser
                        .create856Node(inputParameters.get(DESCRPTION_KEY),
                                inputParameters.get(URL_KEY));

                Document updatedDocument = xmlParser.insertUpdatedIntoRecord(bibRecordXml, updateNode);
                String updatedXml = xmlParser.convertDocToString(updatedDocument);

                /* 6. Push the updated BIB-RECORD back to the alma through a put-request to the api. */
                HttpResponse<String> response = almaConnection.sendPut(mmsId, secretKey, updatedXml);

                if (response.statusCode() == 200) {
                    gatewayResponseBody += "Updated the BIB-post with id: " + mmsId + " in alma. \n";
                    if (othersFailed) {
                        gatewayResponse.setStatusCode(207);
                    } else {
                        gatewayResponse.setStatusCode(200);
                    }
                    othersSucceeded = true;
                } else {
                    gatewayResponseBody += "Failed to updated the BIB-post with id: " + mmsId
                            + ". Alma responded with statuscode: " + response.statusCode() + "\n";
                    if (othersSucceeded) {
                        gatewayResponse.setStatusCode(207);
                    } else {
                        gatewayResponse.setStatusCode(400);
                    }
                    othersFailed = true;
                }
            }
            gatewayResponse.setBody(gatewayResponseBody);
        } catch (ParsingException | IOException | IllegalArgumentException
                | InterruptedException | SecurityException e) {
            DebugUtils.dumpException(e);
            gatewayResponse.setErrorBody(e.getMessage());
            gatewayResponse.setStatusCode(500);
        }
        return gatewayResponse;
    }

    /**
     * Retrieve a list of referenceobjects based on the isbn you enter.
     * @param isbn The isbn you wish to retrieve refrenceobjects based on.
     * @return A list of reference objects matching the isbn, this list will usually contain only one reference object.
     * @throws IOException when something goes wrong
     */
    public List<Reference> getReferenceListByIsbn(String isbn) throws IOException {
        List<Reference> referenceList;
        URL theURL = new URL(Config.getInstance().getAlmaSruEndpoint() + isbn);
        InputStreamReader streamReader = connection.connect(theURL);
        String referenceString = new BufferedReader(streamReader)
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));

        if (referenceString.isEmpty()) {
            return null;
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        Type listOfMyClassObject = new TypeToken<List<Reference>>() {}.getType();
        referenceList = gson.fromJson(referenceString, listOfMyClassObject);
        return referenceList;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> checkParameters(Map<String, Object> input) {
        if (Objects.isNull(input) || !input.containsKey(QUERY_STRING_PARAMETERS_KEY)
                || Objects.isNull(input.get(QUERY_STRING_PARAMETERS_KEY))) {
            throw new ParameterException(MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS);
        }
        Map<String, String> queryStringParameters = (Map<String, String>) input.get(QUERY_STRING_PARAMETERS_KEY);
        if (!queryStringParameters.containsKey(ISBN_KEY)
                || !queryStringParameters.containsKey(DESCRPTION_KEY)
                || !queryStringParameters.containsKey(URL_KEY)
                || Objects.isNull(queryStringParameters.get(ISBN_KEY))
                || Objects.isNull(queryStringParameters.get(DESCRPTION_KEY))
                || Objects.isNull(queryStringParameters.get(URL_KEY))
        ) {
            throw new ParameterException(MANDATORY_PARAMETER_MISSING);
        }
        return queryStringParameters;
    }

}
