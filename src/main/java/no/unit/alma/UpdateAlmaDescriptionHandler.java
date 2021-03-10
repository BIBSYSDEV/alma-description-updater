package no.unit.alma;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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


public class UpdateAlmaDescriptionHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {

    public static final String QUERY_STRING_PARAMETERS_KEY = "queryStringParameters";
    public static final String ISBN_KEY = "isbn";
    public static final String DESCRIPTION_KEY = "description";
    public static final String URL_KEY = "url";
    public static final String RESPONSE_MESSAGE_KEY = "responseMessage";
    public static final String RESPONSE_STATUS_KEY = "responseStatus";
    public static final int STATUS_CODE_200 = 200;

    public static final String MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS =
            "Missing event element 'queryStringParameters'.";
    public static final String MANDATORY_PARAMETER_MISSING =
            "Mandatory parameter 'isbn', 'description' or 'url' is missing.";

    private transient Boolean othersSucceeded = false;
    private transient Boolean othersFailed = false;

    private String secretKey;
    private Map<String, String> inputParameters;
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
        GatewayResponse gatewayResponse = new GatewayResponse();

        Map<String, Object> errorMessage = setUpAndCheckVariables(input);

        if(errorMessage != null){
            gatewayResponse.setErrorBody((String) errorMessage.get(RESPONSE_MESSAGE_KEY));
            gatewayResponse.setStatusCode((int) errorMessage.get(RESPONSE_STATUS_KEY));
        }

        try {
            /* Step 1. Get a REFERENCE LIST from alma-sru through a lambda. */
            List<Reference> referenceList;
            referenceList = getReferenceListByIsbn(inputParameters.get(ISBN_KEY));
            if (referenceList == null) {
                gatewayResponse = createErrorResponse("No reference object retrieved for this ISBN",
                        Response.Status.BAD_REQUEST.getStatusCode());
                return gatewayResponse;
            }
            StringBuilder gatewayResponseBody = new StringBuilder(41);
            gatewayResponseBody.append("Got " + referenceList.size() + " reference object(s) from alma-sru \n");

            AlmaConnection almaConnection = new AlmaConnection();
            DocumentXmlParser xmlParser = new DocumentXmlParser();
            Map<String, Object> responseMap;

            /* 2. Loop through the LIST. */
            for (Reference reference : referenceList) {

                /* 3. Get the MMS_ID from the REFERENCE OBJECT. */
                String mmsId = reference.getId();

                /* 4. Use the MMS_ID to get a BIB-RECORD from the alma-api. */
                HttpResponse<String> almaResponse = almaConnection.sendGet(mmsId, secretKey);
                responseMap = createGatewayResponse(almaResponse.statusCode() == STATUS_CODE_200,
                        "Got the BIB-post for: " + mmsId + "\n",
                        "Couldn't get the BIB-post for: " + mmsId + ". Alma responded with statuscode: "
                        + almaResponse.statusCode() + "\n");
                gatewayResponseBody.append((String) responseMap.get(RESPONSE_MESSAGE_KEY));
                gatewayResponse.setStatusCode((int) responseMap.get(RESPONSE_STATUS_KEY));
                if (almaResponse.statusCode() != STATUS_CODE_200) {
                    othersFailed = true;
                    continue;
                }

                /* 5. Insert the new link-data into the BIB-RECORD. */
                Boolean alreadyExists = xmlParser.alreadyExists(inputParameters.get(DESCRIPTION_KEY),
                        inputParameters.get(URL_KEY), almaResponse.body());
                responseMap = createGatewayResponse(!alreadyExists, "",
                        "409 The BIB-post with mms_id: " + mmsId + " is already up to date. \n");
                if (alreadyExists) {
                    gatewayResponseBody.append((String) responseMap.get(RESPONSE_MESSAGE_KEY));
                    gatewayResponse.setStatusCode((int) responseMap.get(RESPONSE_STATUS_KEY));
                    othersFailed = true;
                    continue;
                }

                Document updateNode = xmlParser.create856Node(inputParameters.get(DESCRIPTION_KEY),
                                inputParameters.get(URL_KEY));

                Document updatedDocument = xmlParser.insertUpdatedIntoRecord(almaResponse.body(), updateNode);
                String updatedXml = xmlParser.convertDocToString(updatedDocument);

                /* 6. Push the updated BIB-RECORD back to the alma through a put-request to the api. */
                HttpResponse<String> response = almaConnection.sendPut(mmsId, secretKey, updatedXml);

                responseMap = createGatewayResponse(response.statusCode() == STATUS_CODE_200,
                        "Updated the BIB-post with id: " + mmsId + " in alma. \n",
                        "Failed to updated the BIB-post with id: " + mmsId
                        + ". Alma responded with statuscode: " + response.statusCode() + "\n");
                gatewayResponseBody.append((String) responseMap.get(RESPONSE_MESSAGE_KEY));
                gatewayResponse.setStatusCode((int) responseMap.get(RESPONSE_STATUS_KEY));

                if (response.statusCode() == STATUS_CODE_200) {
                    othersSucceeded = true;
                } else {
                    othersFailed = true;
                }
            }
            gatewayResponse.setBody(gatewayResponseBody.toString());
        } catch (ParsingException | IOException | IllegalArgumentException
                | InterruptedException | SecurityException e) {
            DebugUtils.dumpException(e);
            gatewayResponse = createErrorResponse(e.getMessage(), 500);
        }
        return gatewayResponse;
    }

    /**
     * A method for assigning values to the inputparameters and secretkey
     * @param input The same input received by the handleRequest method
     * @return returns null if everything works. If not it will return a Map
     * containing an appropriate errormessage and errorsatus.
     */
    private Map<String, Object> setUpAndCheckVariables(Map<String, Object> input){
        Map<String, Object> response = new HashMap<>();
        try {
            Config.getInstance().checkProperties();
            inputParameters = this.checkParameters(input);
        } catch (ParameterException e) {
            DebugUtils.dumpException(e);
            response.put(RESPONSE_MESSAGE_KEY, e.getMessage());
            response.put(RESPONSE_STATUS_KEY, Response.Status.BAD_REQUEST.getStatusCode());
            return response;
        }

        try {
            secretKey = SecretRetriever.getAlmaApiKeySecret();
        } catch (SecretRetrieverException e) {
            response.put(RESPONSE_MESSAGE_KEY, "Couldn't retrieve the API-key " + e.getMessage());
            response.put(RESPONSE_STATUS_KEY, 500);
            return response;
        }
        return null;
    }

    /**
     * Retrieve a list of referenceobjects based on the isbn you enter.
     * @param isbn The isbn you wish to retrieve refrenceobjects based on.
     * @return A list of reference objects matching the isbn, this list will usually contain only one reference object.
     * @throws IOException when something goes wrong
     */
    private List<Reference> getReferenceListByIsbn(String isbn) throws IOException {
        URL theURL = new URL(Config.getInstance().getAlmaSruEndpoint() + isbn);
        InputStreamReader streamReader = new InputStreamReader(theURL.openStream());
        try {
            String referenceString = new BufferedReader(streamReader)
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            streamReader.close();
            if (referenceString.isEmpty()) {
                return null;
            }
            List<Reference> referenceList;
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            Type listOfMyClassObject = new TypeToken<List<Reference>>() {}.getType();
            referenceList = gson.fromJson(referenceString, listOfMyClassObject);
            return referenceList;
        } finally {
            streamReader.close();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> checkParameters(Map<String, Object> input) {
        if (Objects.isNull(input) || !input.containsKey(QUERY_STRING_PARAMETERS_KEY)
                || Objects.isNull(input.get(QUERY_STRING_PARAMETERS_KEY))) {
            throw new ParameterException(MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS);
        }
        Map<String, String> queryStringParameters = (Map<String, String>) input.get(QUERY_STRING_PARAMETERS_KEY);
        if (!queryStringParameters.containsKey(ISBN_KEY)
                || !queryStringParameters.containsKey(DESCRIPTION_KEY)
                || !queryStringParameters.containsKey(URL_KEY)
                || Objects.isNull(queryStringParameters.get(ISBN_KEY))
                || Objects.isNull(queryStringParameters.get(DESCRIPTION_KEY))
                || Objects.isNull(queryStringParameters.get(URL_KEY))
        ) {
            throw new ParameterException(MANDATORY_PARAMETER_MISSING);
        }
        return queryStringParameters;
    }

    /**
     * Assigns the correct message and statusCode based on the input condition.
     * @param condition Boolean that decides which String to use for the message.
     * @param success The String used in case of the condition being true.
     * @param failure The String used in case of the condition being false.
     * @return A Map containing both a message and a statusCode
     */
    public Map<String, Object> createGatewayResponse(Boolean condition, String success, String failure) {
        String responseMessage;
        int responseStatus;
        if (condition) {
            responseMessage = success;
            if (othersFailed) {
                responseStatus = 207;
            } else {
                responseStatus = 200;
            }
        } else {
            responseMessage = failure;
            if (othersSucceeded) {
                responseStatus = 207;
            } else {
                responseStatus = 400;
            }
        }

        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put(RESPONSE_MESSAGE_KEY, responseMessage);
        payload.put(RESPONSE_STATUS_KEY, responseStatus);
        return payload;
    }

    /**
     * Creates a GatewayResponse object with an errormessage and status code.
     * @param errorMessage The errormessage.
     * @param errorCode The status code.
     * @return A gatewayResponse with information about an error.
     */
    public GatewayResponse createErrorResponse(String errorMessage, int errorCode) {
        GatewayResponse gatewayResponse = new GatewayResponse();
        gatewayResponse.setErrorBody(errorMessage);
        gatewayResponse.setStatusCode(errorCode);
        return gatewayResponse;
    }

}
