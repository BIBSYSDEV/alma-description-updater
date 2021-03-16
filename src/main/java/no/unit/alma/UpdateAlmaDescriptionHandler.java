package no.unit.alma;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.http.HttpResponse;
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
import nva.commons.utils.Environment;
import org.w3c.dom.Document;
import software.amazon.awssdk.http.HttpStatusCode;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class UpdateAlmaDescriptionHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {

    public static final String QUERY_STRING_PARAMETERS_KEY = "queryStringParameters";
    public static final String ISBN_KEY = "isbn";
    public static final String SPECIFIEDMATERIAL_KEY = "specifiedMaterial";
    public static final String URL_KEY = "url";
    public static final String RESPONSE_MESSAGE_KEY = "responseMessage";
    public static final String RESPONSE_STATUS_KEY = "responseStatus";
    public static final String ALMA_GET_SUCCESS_MESSAGE = "Got the BIB-post for: ";
    public static final String ALMA_GET_FAILURE_MESSAGE = "Couldn't get the BIB-post for: ";
    public static final String ALMA_SRU_HOST_KEY = "ALMA_SRU_HOST";
    public static final String ALMA_API_KEY = "ALMA_API_HOST";

    public static final String MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS =
            "Missing event element 'queryStringParameters'.";
    public static final String MANDATORY_PARAMETER_MISSING =
            "Mandatory parameter 'isbn', 'specifiedMaterial' or 'url' is missing.";
    public static final String ALMA_GET_RESPONDED_WITH_STATUSCODE = ". Alma responded with statuscode: ";
    public static final String ALMA_PUT_SUCCESS_MESSAGE = "Updated the BIB-post in alma with id: ";
    public static final String ALMA_PUT_FAILURE_MESSAGE = "Failed to updated the BIB-post with id: ";
    public static final String ALMA_POST_ALREADY_UPDATED = "The BIB-post with is already up-to-date, post with mms_id: ";
    public static final int RESPONSE_STATUS_MULTI_STATUS_CODE = 207;
    public static final String NO_REFERENCE_OBJECT_RETRIEVED_MESSAGE = "No reference object retrieved for this ISBN";
    public static final String NUMBER_OF_REFERENCE_OBJECTS_MESSAGE = " reference object(s) retrieved from alma-sru.";

    private transient Boolean othersSucceeded = false;
    private transient Boolean othersFailed = false;

    private transient String secretKey;
    private transient Map<String, String> inputParameters;
    private transient final Environment envHandler;
    private transient String almaApiHost;
    private transient String almaSruHost;

    public UpdateAlmaDescriptionHandler(Environment envHandler) { this.envHandler = envHandler; };
    public UpdateAlmaDescriptionHandler() { envHandler = new Environment(); }

    /**
     * Main lambda function to update the links in Alma records.
     * Program flow:
     * 1. Get a REFERENCE LIST from alma-sru through a lambda.
     * 2. Loop through the LIST (and do the following for every OBJECT).
     * 3. Get the MMS_ID from the REFERENCE OBJECT.
     * 4. Use the MMS_ID to get a BIB-RECORD from the alma-api.
     * 5. Determine whether the post is electronic or print.
     * 6. Insert the new link-data into the BIB-RECORD.
     * 7. Push the updated BIB-RECORD back to the alma through a put-request to the api.
     * @param input payload with identifying parameters
     * @return a GatewayResponse
     */
    @Override
    @SuppressWarnings("unchecked")
    public GatewayResponse handleRequest(final Map<String, Object> input, Context context) {
        GatewayResponse gatewayResponse = new GatewayResponse();

        Map<String, Object> errorMessage = initVariables(input);

        if(errorMessage != null) {
            gatewayResponse.setErrorBody((String) errorMessage.get(RESPONSE_MESSAGE_KEY));
            gatewayResponse.setStatusCode((int) errorMessage.get(RESPONSE_STATUS_KEY));
            return gatewayResponse;
        }

        try {
            /* Step 1. Get a REFERENCE LIST from alma-sru through a lambda. */
            List<Reference> referenceList = getReferenceListByIsbn(inputParameters.get(ISBN_KEY));
            if (referenceList == null) {
                gatewayResponse = createErrorResponse(NO_REFERENCE_OBJECT_RETRIEVED_MESSAGE,
                        Response.Status.BAD_REQUEST.getStatusCode());
                return gatewayResponse;
            }
            StringBuilder gatewayResponseBody = new StringBuilder(41);
            gatewayResponseBody.append(referenceList.size() + NUMBER_OF_REFERENCE_OBJECTS_MESSAGE + System.lineSeparator());

            DocumentXmlParser xmlParser = new DocumentXmlParser();

            /* 2. Loop through the LIST. */
            for (Reference reference : referenceList) {

                /* 3. Get the MMS_ID from the REFERENCE OBJECT. */
                String mmsId = reference.getId();

                /* 4. Use the MMS_ID to get a BIB-RECORD from the alma-api. */
                HttpResponse<String> almaResponse = getBibRecordFromAlma(gatewayResponse, gatewayResponseBody, mmsId);
                if (almaResponse.statusCode() != HttpStatusCode.OK) {
                    othersFailed = true;
                    continue;
                }

                /* 5. Determine whether the post is electronic or print. */
                int marcTag = xmlParser.determineElectronicOrPrint(almaResponse.body());

                /* 6. Insert the new link-data into the BIB-RECORD. */
                Boolean alreadyExists = xmlParser.alreadyExists(inputParameters.get(SPECIFIEDMATERIAL_KEY),
                        inputParameters.get(URL_KEY), almaResponse.body(), marcTag);
                if (alreadyExists) {
                    gatewayResponseBody.append(ALMA_POST_ALREADY_UPDATED + mmsId);
                    gatewayResponse.setStatusCode(HttpStatusCode.BAD_REQUEST);
                    othersFailed = true;
                    continue;
                }

                Document updateNode = xmlParser.createNode(inputParameters.get(SPECIFIEDMATERIAL_KEY),
                                inputParameters.get(URL_KEY), marcTag);

                Document updatedDocument = xmlParser.insertUpdatedIntoRecord(almaResponse.body(), updateNode, marcTag);
                String updatedXml = xmlParser.convertDocToString(updatedDocument);

                /* 7. Push the updated BIB-RECORD back to the alma through a put-request to the api. */
                HttpResponse<String> response = putBibRecordInAlma(gatewayResponse, gatewayResponseBody, mmsId, updatedXml);

                if (response.statusCode() == HttpStatusCode.OK) {
                    othersSucceeded = true;
                } else {
                    othersFailed = true;
                }
            }
            gatewayResponse.setBody(gatewayResponseBody.toString());
        } catch (ParsingException | IOException | IllegalArgumentException
                | InterruptedException | SecurityException e) {
            DebugUtils.dumpException(e);
            gatewayResponse = createErrorResponse(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
        return gatewayResponse;
    }

    /**
     * A method that sends a get request to ALMA.
     * @param gatewayResponse The main response object.
     * @param gatewayResponseBody The stringbuilder used to build a response body.
     * @param mmsId The mms id needed to specify which post to retrieve.
     * @return A http response mirroring the response from the get request sent to ALMA.
     * @throws InterruptedException When something goes wrong.
     * @throws IOException When something goes wrong.
     */
    private HttpResponse<String> getBibRecordFromAlma(GatewayResponse gatewayResponse, StringBuilder gatewayResponseBody, String mmsId) throws InterruptedException, IOException {
        Map<String, Object> responseMap;
        HttpResponse<String> almaResponse = AlmaConnection.getInstance().sendGet(mmsId, secretKey, almaApiHost);
        responseMap = createGatewayResponse(almaResponse.statusCode() == HttpStatusCode.OK,
                ALMA_GET_SUCCESS_MESSAGE + mmsId + System.lineSeparator(),
                ALMA_GET_FAILURE_MESSAGE + mmsId + ALMA_GET_RESPONDED_WITH_STATUSCODE
                        + almaResponse.statusCode() + System.lineSeparator());
        gatewayResponseBody.append((String) responseMap.get(RESPONSE_MESSAGE_KEY));
        gatewayResponse.setStatusCode((int) responseMap.get(RESPONSE_STATUS_KEY));
        return almaResponse;
    }

    /**
     * A method that sends a put request to ALMA.
     * @param gatewayResponse The main response object.
     * @param gatewayResponseBody The stringbuilder used to build a response body.
     * @param mmsId The mms id needed to specify which post to update.
     * @param updatedXml The string which we want to update the post with.
     * @return A http response mirroring the response from the put request sent to ALMA.
     * @throws InterruptedException When something goes wrong.
     * @throws IOException When something goes wrong.
     */
    private HttpResponse<String> putBibRecordInAlma(GatewayResponse gatewayResponse, StringBuilder gatewayResponseBody, String mmsId, String updatedXml) throws InterruptedException, IOException {
        Map<String, Object> responseMap;
        HttpResponse<String> almaResponse = AlmaConnection.getInstance().sendPut(mmsId, secretKey, updatedXml, almaApiHost);
        responseMap = createGatewayResponse(almaResponse.statusCode() == HttpStatusCode.OK,
                ALMA_PUT_SUCCESS_MESSAGE + mmsId + System.lineSeparator(),
                ALMA_PUT_FAILURE_MESSAGE + mmsId + ALMA_GET_RESPONDED_WITH_STATUSCODE
                        + almaResponse.statusCode() + System.lineSeparator());
        gatewayResponseBody.append((String) responseMap.get(RESPONSE_MESSAGE_KEY));
        gatewayResponse.setStatusCode((int) responseMap.get(RESPONSE_STATUS_KEY));
        return almaResponse;
    }

    /**
     * A method for assigning values to the inputparameters and secretkey
     * @param input The same input received by the handleRequest method
     * @return returns null if everything works. If not it will return a Map
     * containing an appropriate errormessage and errorsatus.
     */
    private Map<String, Object> initVariables(Map<String, Object> input) {
        Map<String, Object> response = new ConcurrentHashMap<>();
        try {
            checkProperties();
            inputParameters = this.checkParameters(input);
        } catch (RuntimeException e) {
            DebugUtils.dumpException(e);
            response.put(RESPONSE_MESSAGE_KEY, e.getMessage());
            response.put(RESPONSE_STATUS_KEY, Response.Status.BAD_REQUEST.getStatusCode());
            return response;
        }

        try {
            secretKey = SecretRetriever.getAlmaApiKeySecret();
        } catch (SecretRetrieverException e) {
            response.put(RESPONSE_MESSAGE_KEY, "Couldn't retrieve the API-key " + e.getMessage());
            response.put(RESPONSE_STATUS_KEY, HttpStatusCode.INTERNAL_SERVER_ERROR);
            return response;
        }
        return null;
    }

    public boolean checkProperties() {
        almaApiHost = envHandler.readEnv(ALMA_API_KEY);
        almaSruHost = envHandler.readEnv(ALMA_SRU_HOST_KEY);
        return true;
    }

    /**
     * Retrieve a list of referenceobjects based on the isbn you enter.
     * @param isbn The isbn you wish to retrieve refrenceobjects based on.
     * @return A list of reference objects matching the isbn, this list will usually contain only one reference object.
     * @throws IOException when something goes wrong
     */
    private List<Reference> getReferenceListByIsbn(String isbn) throws IOException {
        URL theURL = new URL(almaSruHost + isbn);
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
                || !queryStringParameters.containsKey(SPECIFIEDMATERIAL_KEY)
                || !queryStringParameters.containsKey(URL_KEY)
                || Objects.isNull(queryStringParameters.get(ISBN_KEY))
                || Objects.isNull(queryStringParameters.get(SPECIFIEDMATERIAL_KEY))
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
                responseStatus = RESPONSE_STATUS_MULTI_STATUS_CODE;
            } else {
                responseStatus = HttpStatusCode.OK;
            }
        } else {
            responseMessage = failure;
            if (othersSucceeded) {
                responseStatus = RESPONSE_STATUS_MULTI_STATUS_CODE;
            } else {
                responseStatus = HttpStatusCode.BAD_REQUEST;
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
