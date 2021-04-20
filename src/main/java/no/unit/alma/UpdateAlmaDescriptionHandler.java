package no.unit.alma;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import no.unit.scheduler.SchedulerHelper;
import no.unit.scheduler.UpdateItem;
import no.unit.exceptions.SqsException;
import no.unit.exceptions.ParsingException;
import no.unit.exceptions.SecretRetrieverException;
import no.unit.marc.Reference;
import no.unit.secret.SecretRetriever;
import no.unit.utils.DebugUtils;
import nva.commons.utils.Environment;
import org.w3c.dom.Document;
import software.amazon.awssdk.http.HttpStatusCode;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class UpdateAlmaDescriptionHandler implements RequestHandler<SQSEvent, Void> {

    public static final String ALMA_SRU_HOST_KEY = "ALMA_SRU_HOST";
    public static final String ALMA_API_KEY = "ALMA_API_HOST";

    private transient String secretKey;
    private final transient  Environment envHandler;
    private transient String almaApiHost;
    private transient String almaSruHost;
    private final transient SchedulerHelper schedulerHelper = new SchedulerHelper();
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
     * 1. Create an UpdateItem LIST from the input.
     * 2. Get a REFERENCE LIST from alma-sru through a lambda.
     * 3. Loop through the REFERENCE LIST (and do the following for every OBJECT).
     * 3.1 Get the MMS_ID from the REFERENCE OBJECT.
     * 3.2 Use the MMS_ID to get a BIB-RECORD from the alma-api.
     * 3.3 Create an XML(String) by updating the existing ALMA xml with all the UpdateItems.
     * 3.3.1 Loop through every UpdateItem in the UpdateItem LIST.
     * 3.3.2 Determine whether the post is electronic or print.
     * 3.3.3 Check if the update already exists.
     * 3.3.4 Create a node from the UpdateItem.
     * 3.3.5 Insert update node into the record retrieved from ALMA.
     * 4. Push the updated BIB-RECORD back to the alma through a put-request to the api.
     * @param event payload with identifying parameters
     * @return a GatewayResponse
     */
    @Override
    @SuppressWarnings("unchecked")
    public Void handleRequest(final SQSEvent event, Context context) {

        try{
            initVariables();
        } catch(SecretRetrieverException | IllegalStateException e) {
            //TODO Throw an exception
        }

        /* 1. Create an UpdateItem LIST from the input. */
        List<UpdateItem> updateItems;

        try {
            updateItems = schedulerHelper.splitEventIntoUpdateItems(event.getRecords().get(0).getBody());
        } catch (Exception e) {
            //TODO Throw an exception
            return null;
        }

        try {
            /* Step 2. Get a REFERENCE LIST from alma-sru through a lambda. */
            List<Reference> referenceList = getReferenceListByIsbn(updateItems.get(0).getIsbn());
            if (referenceList == null) {
                //TODO Throw an exception
            }

            /* 3. Loop through the LIST. */
            for (Reference reference : referenceList) {

                /* 3.1 Get the MMS_ID from the REFERENCE OBJECT. */
                String mmsId = reference.getId();

                /* 3.2 Use the MMS_ID to get a BIB-RECORD from the alma-api. */
                HttpResponse<String> almaResponse = getBibRecordFromAlmaWithRetries(mmsId, event);

                if (almaResponse == null) {
                    return null;
                }

                String xmlFromAlma = almaResponse.body();

                /* 3.3 Create an XML(String) by updating the existing ALMA xml with all the updateItems. */
                String updatedRecord = updateBibRecord(updateItems, xmlFromAlma);

                /* 4. Push the updated BIB-RECORD back to the alma through a put-request to the api. */
                HttpResponse<String> response = putBibRecordInAlmaWithRetries(mmsId, updatedRecord, event);

                if(response == null) {
                    return  null;
                }
            }
        } catch (ParsingException | IOException | IllegalArgumentException
                | InterruptedException | SecurityException | SqsException e) {
            DebugUtils.dumpException(e);
            //TODO Throw an exception
        }
        return null;
    }

    /**
     * Create an XML(String) by updating the existing ALMA xml with all the UpdateItems.
     * @param updateItems A list of UpdateItems.
     * @param xmlFromAlma A String in the shape of an XML the data is retrieved from ALMA.
     * @return The same XML data that was entered now with added fields (either 856 or 956).
     * @throws ParsingException When something goes wrong.
     */
    public String updateBibRecord(List<UpdateItem> updateItems, String xmlFromAlma) throws ParsingException {
        String xmlBuilderString = xmlFromAlma;
        /* 3.3.1 Loop through every UpdateItem in the UpdateItem LIST. */
        for (UpdateItem item : updateItems) {
            /* 3.3.2 Determine whether the post is electronic or print. */
            int marcTag = xmlParser.determineElectronicOrPrint(xmlBuilderString);

            /* 3.3.3 Check if the update already exists. */
            Boolean alreadyExists = xmlParser.alreadyExists(item.getSpecifiedMaterial(),
                    item.getLink(), xmlBuilderString, marcTag);
            if (alreadyExists) {
                continue;
            }

            /* 3.3.4 Create a node from the UpdateItem. */
            Document updateNode = xmlParser.createNode(item.getSpecifiedMaterial(),
                    item.getLink(), marcTag);

            /* 3.3.5 Insert update node into the record retrieved from ALMA. */
            Document updatedDocument = xmlParser.insertUpdatedIntoRecord(xmlBuilderString,
                    updateNode, marcTag);
            xmlBuilderString = xmlParser.convertDocToString(updatedDocument);

        }
        return xmlBuilderString;
    }

    /**
     * A method that sends a get request to ALMA.
     * @param mmsId The mms id needed to specify which post to retrieve.
     * @return A http response mirroring the response from the get request sent to ALMA.
     * @throws InterruptedException When something goes wrong.
     * @throws IOException When something goes wrong.
     */
    private HttpResponse<String> getBibRecordFromAlma(String mmsId)
            throws InterruptedException, IOException {
        HttpResponse<String> almaResponse = AlmaConnection.getInstance().sendGet(mmsId, secretKey, almaApiHost);
        return almaResponse;
    }

    /**
     * A method that sends a put request to ALMA.
     * @param mmsId The mms id needed to specify which post to update.
     * @param updatedXml The string which we want to update the post with.
     * @return A http response mirroring the response from the put request sent to ALMA.
     * @throws InterruptedException When something goes wrong.
     * @throws IOException When something goes wrong.
     */
    private HttpResponse<String> putBibRecordInAlma(String mmsId, String updatedXml)
            throws InterruptedException, IOException {
        HttpResponse<String> almaResponse = AlmaConnection.getInstance().sendPut(mmsId, secretKey,
                updatedXml, almaApiHost);
        return almaResponse;
    }

    /**
     * Method to retry GET-calls to ALMA, sleeps for 3 seconds before retrying.
     * @param mmsId For identifying the record in ALMA.
     * @return HttpResponse<String> with the ALMA response or null if failing.
     * @throws InterruptedException when the sleep is interrupted.
     */
    public HttpResponse<String> getBibRecordFromAlmaWithRetries(String mmsId, SQSEvent event)
            throws InterruptedException, SqsException {
        HttpResponse<String> almaResponse = null;
        try {

            almaResponse = getBibRecordFromAlma(mmsId);
        } catch (InterruptedException | IOException e) {
            almaResponse = null;
        }

        if (almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK) {
            return almaResponse;
        } else {
            TimeUnit.SECONDS.sleep(3);
            try {
                almaResponse = getBibRecordFromAlma(mmsId);
            } catch (InterruptedException | IOException e) {
                almaResponse = null;
            }
            if (almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK) {
                return almaResponse;
            } else {
                TimeUnit.SECONDS.sleep(3);
                try{
                    almaResponse = getBibRecordFromAlma(mmsId);
                } catch (InterruptedException | IOException e) {
                    almaResponse = null;
                }
                if (almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK) {
                    return almaResponse;
                } else {
                    schedulerHelper.writeToDLQ(event.getRecords().get(0).getBody());
                    return null;
                }
            }
        }
    }

    /**
     * Method to retry Put-calls to ALMA, sleeps for 3 seconds before retrying.
     * @param mmsId For identifying the record in ALMA.
     * @return HttpResponse<String> with the ALMA response or null if failing.
     * @throws InterruptedException when the sleep is interrupted.
     */
    public HttpResponse<String> putBibRecordInAlmaWithRetries(String mmsId, String updatedRecord, SQSEvent event)
            throws InterruptedException, SqsException {
        HttpResponse<String> response = null;
        try {
            response = putBibRecordInAlma(mmsId, updatedRecord);
        } catch (InterruptedException | IOException e) {
            response = null;
        }
        if (response != null && response.statusCode() == HttpStatusCode.OK) {
            return response;
        } else {
            TimeUnit.SECONDS.sleep(3);
            try {
                response = putBibRecordInAlma(mmsId, updatedRecord);
            } catch (InterruptedException | IOException e) {
                response = null;
            }
            if (response != null && response.statusCode() == HttpStatusCode.OK) {
                return response;
            } else {
                TimeUnit.SECONDS.sleep(3);
                try {
                    response = putBibRecordInAlma(mmsId, updatedRecord);
                } catch (InterruptedException | IOException e) {
                    response = null;
                }
                if (response != null && response.statusCode() == HttpStatusCode.OK) {
                    return response;
                } else {
                    schedulerHelper.writeToDLQ(event.getRecords().get(0).getBody());
                    return null;
                }
            }
        }
    }

    /**
     * A method for assigning values to the secretkey and checking the environment variables.
     * @return returns null if everything works. If not it will return a Map
     *     containing an appropriate errormessage and errorsatus.
     */
    private void initVariables() throws IllegalStateException, SecretRetrieverException {
        readEnvVariables();
        secretKey = SecretRetriever.getAlmaApiKeySecret();
    }

    /**
     * Stores the systemvariable properties.
     * @return It returnes a boolean indicating that it retrieved the systemvariables.
     */
    public boolean readEnvVariables() throws IllegalStateException {
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

}
