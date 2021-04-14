package no.unit.alma;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import no.unit.dynamo.DynamoDbConnection;
import no.unit.dynamo.DynamoDbHelperClass;
import no.unit.dynamo.DynamoDbItem;
import no.unit.dynamo.UpdatePayload;
import no.unit.exceptions.DynamoDbException;
import no.unit.exceptions.ParsingException;
import no.unit.exceptions.SecretRetrieverException;
import no.unit.marc.Reference;
import no.unit.secret.SecretRetriever;
import no.unit.utils.DebugUtils;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.w3c.dom.Document;
import software.amazon.awssdk.http.HttpStatusCode;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class UpdateAlmaDescriptionHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {

    public static final String RESPONSE_MESSAGE_KEY = "responseMessage";
    public static final String RESPONSE_STATUS_KEY = "responseStatus";
    public static final String ALMA_GET_SUCCESS_MESSAGE = "Got the BIB-post for: ";
    public static final String ALMA_GET_FAILURE_MESSAGE = "Couldn't get the BIB-post for: ";
    public static final String ALMA_SRU_HOST_KEY = "ALMA_SRU_HOST";
    public static final String ALMA_API_KEY = "ALMA_API_HOST";
    public static final String MODIFIED_KEY = "modified";

    public static final String ALMA_GET_RESPONDED_WITH_STATUSCODE = ". Alma responded with statuscode: ";
    public static final String ALMA_PUT_SUCCESS_MESSAGE = "Updated the BIB-post in alma with id: ";
    public static final String ALMA_PUT_FAILURE_MESSAGE = "Failed to updated the BIB-post with id: ";
    public static final String ALMA_POST_ALREADY_UPDATED = "The BIB-post with is already up-to-date, "
            + "post with mms_id: ";
    public static final String NO_REFERENCE_OBJECT_RETRIEVED_MESSAGE = "No reference object retrieved for this ISBN";
    public static final String NUMBER_OF_REFERENCE_OBJECTS_MESSAGE = " reference object(s) retrieved from alma-sru.";

    private transient String secretKey;
    private final transient  Environment envHandler;
    private transient String almaApiHost;
    private transient String almaSruHost;
    private final transient DynamoDbConnection dbConnection = new DynamoDbConnection();
    private final transient DynamoDbHelperClass dynamoDbHelper = new DynamoDbHelperClass();
    private final transient DocumentXmlParser xmlParser = new DocumentXmlParser();

    public UpdateAlmaDescriptionHandler(Environment envHandler) {
        this.envHandler = envHandler;
    }

    public UpdateAlmaDescriptionHandler() {
        envHandler = new Environment();
    }

    /**
     * Main lambda function to update the links in Alma records.
     * Program flow:
     * 1. Create an UpdatePayload LIST from the input.
     * 2. Get a REFERENCE LIST from alma-sru through a lambda.
     * 3. Loop through the REFERENCE LIST (and do the following for every OBJECT).
     * 3.1 Get the MMS_ID from the REFERENCE OBJECT.
     * 3.2 Use the MMS_ID to get a BIB-RECORD from the alma-api.
     * 3.3 Create an XML(String) by updating the existing ALMA xml with all the updatePayload items.
     * 3.3.1 Loop through every UpdatePayload in the UpdatePayload LIST.
     * 3.3.2 Determine whether the post is electronic or print.
     * 3.3.3 Check if the update already exists.
     * 3.3.4 Create a node from the UpdatePayload item.
     * 3.3.5 Insert update node into the record retrieved from ALMA.
     * 4. Push the updated BIB-RECORD back to the alma through a put-request to the api.
     * @param input payload with identifying parameters
     * @return a GatewayResponse
     */
    @Override
    @SuppressWarnings("unchecked")
    public GatewayResponse handleRequest(final Map<String, Object> input, Context context) {
        GatewayResponse gatewayResponse = new GatewayResponse();

        Map<String, Object> errorMessage = initVariables();

        if (errorMessage != null) {
            gatewayResponse.setErrorBody((String) errorMessage.get(RESPONSE_MESSAGE_KEY));
            gatewayResponse.setStatusCode((int) errorMessage.get(RESPONSE_STATUS_KEY));
            return gatewayResponse;
        }

        /* 1. Create an UpdatePayload LIST from the input. */
        List<UpdatePayload> payloadItems;

        try {
            payloadItems = getPayloadList();
        } catch (DynamoDbException e) {
            gatewayResponse = createErrorResponse(e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return gatewayResponse;
        }

        StringBuilder gatewayResponseBody = new StringBuilder(41);

        try {
            /* Step 2. Get a REFERENCE LIST from alma-sru through a lambda. */
            List<Reference> referenceList = getReferenceListByIsbn(payloadItems.get(0).getIsbn());
            if (referenceList == null) {
                gatewayResponseBody.append(NO_REFERENCE_OBJECT_RETRIEVED_MESSAGE + payloadItems.get(0).getIsbn());
                gatewayResponse.setBody(gatewayResponseBody.toString());
                gatewayResponse.setStatusCode(400);
                return gatewayResponse;
            }
            gatewayResponseBody.append(referenceList.size()).append(NUMBER_OF_REFERENCE_OBJECTS_MESSAGE)
                    .append(System.lineSeparator());

            /* 3. Loop through the LIST. */
            for (Reference reference : referenceList) {

                /* 3.1 Get the MMS_ID from the REFERENCE OBJECT. */
                String mmsId = reference.getId();

                HttpResponse<String> almaResponse = null;
                /* 3.2 Use the MMS_ID to get a BIB-RECORD from the alma-api. */
                almaResponse = getBibRecordFromAlma(gatewayResponse,
                        gatewayResponseBody, mmsId);
                if (almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK) {
                    gatewayResponseBody.append("Record with mms_id: " + mmsId + " found in ALMA");
                } else {
                    TimeUnit.SECONDS.sleep(3);
                    almaResponse = getBibRecordFromAlma(gatewayResponse,
                            gatewayResponseBody, mmsId);
                    if (almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK) {
                        gatewayResponseBody.append("Record with mms_id: " + mmsId + " found in ALMA");
                    } else {
                        TimeUnit.SECONDS.sleep(3);
                        almaResponse = getBibRecordFromAlma(gatewayResponse,
                                gatewayResponseBody, mmsId);
                        if (almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK) {
                            gatewayResponseBody.append("Record with mms_id: " + mmsId + " found in ALMA");
                        } else {
                            //TODO Write to SQS
                        }
                    }
                }

                String xmlFromAlma = almaResponse.body();

                /* 3.3 Create an XML(String) by updating the existing ALMA xml with all the updatePayload items. */
                String updatedRecord = updateBibRecord(payloadItems, xmlFromAlma);

                HttpResponse<String> response = null;
                /* 4. Push the updated BIB-RECORD back to the alma through a put-request to the api. */
                response = putBibRecordInAlma(gatewayResponse, gatewayResponseBody, mmsId,
                        updatedRecord);

                if (response != null && response.statusCode() == HttpStatusCode.OK) {
                    gatewayResponseBody.append(mmsId).append(": Has been updated").append(System.lineSeparator());
                } else {
                    TimeUnit.SECONDS.sleep(3);
                    response = putBibRecordInAlma(gatewayResponse, gatewayResponseBody, mmsId,
                            updatedRecord);
                    if (response != null && response.statusCode() == HttpStatusCode.OK) {
                        gatewayResponseBody.append(mmsId).append(": Has been updated").append(System.lineSeparator());
                    } else {
                        TimeUnit.SECONDS.sleep(3);
                        response = putBibRecordInAlma(gatewayResponse, gatewayResponseBody, mmsId,
                                updatedRecord);
                        if (response != null && response.statusCode() == HttpStatusCode.OK) {
                            gatewayResponseBody.append(mmsId).append(": Has been updated").append(System.lineSeparator());
                        } else {
                            //TODO Write to SQS
                        }
                    }
                }
            }
            gatewayResponse.appendBody(gatewayResponseBody.toString());
        } catch (ParsingException | IOException | IllegalArgumentException
                | InterruptedException | SecurityException e) {
            DebugUtils.dumpException(e);
            gatewayResponse.appendBody(e.getMessage());
        }

        return gatewayResponse;
    }

    /**
     * Create a list of UpdatePayload items based on items retrieved from dynamoDB.
     * @return A list of UpdatePayload items.
     * @throws DynamoDbException When something goes wrong.
     */
    @JacocoGenerated
    public List<UpdatePayload> getPayloadList() throws DynamoDbException {
        List<DynamoDbItem> dynamoDbItems = dbConnection.getAllRecordsFromYesterday(MODIFIED_KEY);
        List<UpdatePayload> payloadList = dynamoDbHelper.createLinks(dynamoDbItems);
        return payloadList;
    }

    /**
     * Create an XML(String) by updating the existing ALMA xml with all the updatePayload items.
     * @param updatePayloadItems A list of UpdatePayload items.
     * @param xmlFromAlma A String in the shape of an XML the data is retrieved from ALMA.
     * @return The same XML data that was entered now with added fields (either 856 or 956).
     * @throws ParsingException When something goes wrong.
     */
    public String updateBibRecord(List<UpdatePayload> updatePayloadItems, String xmlFromAlma) throws ParsingException {
        String xmlBuilderString = xmlFromAlma;
        /* 3.3.1 Loop through every UpdatePayload in the UpdatePayload LIST. */
        for (UpdatePayload payloadItem : updatePayloadItems) {
            /* 3.3.2 Determine whether the post is electronic or print. */
            int marcTag = xmlParser.determineElectronicOrPrint(xmlBuilderString);

            /* 3.3.3 Check if the update already exists. */
            Boolean alreadyExists = xmlParser.alreadyExists(payloadItem.getSpecifiedMaterial(),
                    payloadItem.getLink(), xmlBuilderString, marcTag);
            if (alreadyExists) {
                continue;
            }

            /* 3.3.4 Create a node from the UpdatePayload item. */
            Document updateNode = xmlParser.createNode(payloadItem.getSpecifiedMaterial(),
                    payloadItem.getLink(), marcTag);

            /* 3.3.5 Insert update node into the record retrieved from ALMA. */
            Document updatedDocument = xmlParser.insertUpdatedIntoRecord(xmlBuilderString,
                    updateNode, marcTag);
            xmlBuilderString = xmlParser.convertDocToString(updatedDocument);

        }
        return xmlBuilderString;
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
    private HttpResponse<String> getBibRecordFromAlma(GatewayResponse gatewayResponse,
                                                      StringBuilder gatewayResponseBody, String mmsId)
            throws InterruptedException, IOException {
        Map<String, Object> responseMap;
        HttpResponse<String> almaResponse = AlmaConnection.getInstance().sendGet(mmsId, secretKey, almaApiHost);
        responseMap = createGatewayResponse(almaResponse.statusCode(),
                ALMA_GET_SUCCESS_MESSAGE + mmsId + System.lineSeparator(),
                ALMA_GET_FAILURE_MESSAGE + mmsId + ALMA_GET_RESPONDED_WITH_STATUSCODE
                        + almaResponse.statusCode() + System.lineSeparator());
        gatewayResponseBody.append((String) responseMap.get(RESPONSE_MESSAGE_KEY)).append(System.lineSeparator());
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
    private HttpResponse<String> putBibRecordInAlma(GatewayResponse gatewayResponse,
                                                    StringBuilder gatewayResponseBody, String mmsId, String updatedXml)
            throws InterruptedException, IOException {
        Map<String, Object> responseMap;
        HttpResponse<String> almaResponse = AlmaConnection.getInstance().sendPut(mmsId, secretKey,
                updatedXml, almaApiHost);
        responseMap = createGatewayResponse(almaResponse.statusCode(),
                ALMA_PUT_SUCCESS_MESSAGE + mmsId + System.lineSeparator(),
                ALMA_PUT_FAILURE_MESSAGE + mmsId + ALMA_GET_RESPONDED_WITH_STATUSCODE
                        + almaResponse.statusCode() + System.lineSeparator());
        gatewayResponseBody.append((String) responseMap.get(RESPONSE_MESSAGE_KEY)).append(System.lineSeparator());
        gatewayResponse.setStatusCode((int) responseMap.get(RESPONSE_STATUS_KEY));
        return almaResponse;
    }

    /**
     * A method for assigning values to the secretkey and checking the environment variables.
     * @return returns null if everything works. If not it will return a Map
     *     containing an appropriate errormessage and errorsatus.
     */
    private Map<String, Object> initVariables() {
        Map<String, Object> response = new ConcurrentHashMap<>();
        try {
            checkProperties();
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

    /**
     * Stores the systemvariable properties.
     * @return It returnes a boolean indicating that it retrieved the systemvariables.
     */
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


    /**
     * Assigns the correct message and statusCode based on the input condition.
     * @param status Status-code to determine what String input to use.
     * @param success The String used in case of the condition being true.
     * @param failure The String used in case of the condition being false.
     * @return A Map containing both a message and a statusCode
     */
    public Map<String, Object> createGatewayResponse(int status, String success, String failure) {
        String responseMessage;
        int responseStatus;
        if (status == HttpStatusCode.OK) {
            responseMessage = success;
            responseStatus = HttpStatusCode.OK;
        } else {
            responseMessage = failure;
            responseStatus = HttpStatusCode.BAD_REQUEST;
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
