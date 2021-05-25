package no.unit.alma;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import no.unit.scheduler.SchedulerHelper;
import no.unit.scheduler.UpdateItem;
import no.unit.exceptions.SchedulerException;
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
    private final transient AlmaHelper almaHelper = new AlmaHelper();
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
    @SuppressWarnings({"unchecked", "PMD.NPathComplexity"})
    public Void handleRequest(final SQSEvent event, Context context) {

        try {
            initVariables();
        } catch (SchedulerException e) {
            throw new RuntimeException("Error while setting up env-variables and secretKeys. " + e.getMessage());
        }

        /* 1. Create an UpdateItem LIST from the input. */
        List<UpdateItem> updateItems;

        try {
            updateItems = schedulerHelper.splitEventIntoUpdateItems(event.getRecords().get(0).getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error while processing input event. " + e.getMessage());
        }

        if (updateItems.isEmpty()) {
            //In case we recieve an update without any relevant information
            // (at the time this include audiofiles) we just skip them.
            return null;
        }

        try {
            /* Step 2. Get a REFERENCE LIST from alma-sru through a lambda. */
            List<Reference> referenceList = getReferenceListByIsbn(updateItems.get(0).getIsbn());
            if (referenceList == null || referenceList.isEmpty()) {
                referenceList = getReferenceListByIsbn(almaHelper.convertIsbn(updateItems.get(0).getIsbn()));
                if (referenceList == null || referenceList.isEmpty()) {
                    schedulerHelper.writeToDLQ(event.getRecords().get(0).getBody());
                    return null;
                }
            }

            HttpResponse<String> almaResponse = null;
            HttpResponse<String> response = null;
            int sucessCounter = 0;
            /* 3. Loop through the LIST. */
            for (Reference reference : referenceList) {

                System.out.println("Found " + referenceList.size() + " different posts for the isbn: " + updateItems.get(0).getIsbn());
                /* 3.1 Get the MMS_ID from the REFERENCE OBJECT. */
                String mmsId = reference.getId();

                /* 3.2 Use the MMS_ID to get a BIB-RECORD from the alma-api. */
                almaResponse = almaHelper
                        .getBibRecordFromAlmaWithRetries(mmsId, secretKey, almaApiHost);

                if (almaResponse == null || almaResponse.statusCode() != HttpStatusCode.OK) {
                    continue;
                }

                String xmlFromAlma = almaResponse.body();

                /* 3.3 Create an XML(String) by updating the existing ALMA xml with all the updateItems. */
                String updatedRecord = updateBibRecord(updateItems, xmlFromAlma);

                /* 4. Push the updated BIB-RECORD back to the alma through a put-request to the api. */
                response = almaHelper
                        .putBibRecordInAlmaWithRetries(mmsId, updatedRecord, secretKey, almaApiHost);

                if (response == null || response.statusCode() != HttpStatusCode.OK) {
                    continue;
                }
                System.out.println("Completed the update in Alma for post with mms_id: " + mmsId);
                sucessCounter++;
            }
            if (sucessCounter < referenceList.size()) {
                if (almaResponse == null) {
                    throw new RuntimeException("1 or more mms_id's did not go through with mms_id: "
                            + updateItems.get(0).getIsbn()
                            + System.lineSeparator() + "Get failed");

                }
                if (response == null) {
                    throw new RuntimeException("1 or more mms_id's did not go through with mms_id: "
                            + updateItems.get(0).getIsbn()
                            + System.lineSeparator() + "Get response " + almaResponse.body());

                }
                throw new RuntimeException("1 or more mms_id's did not go through with mms_id: "
                        + updateItems.get(0).getIsbn()
                        + System.lineSeparator() + "Get response " + almaResponse.body()
                        + "Put response: " + response.body());
            }

        } catch (ParsingException | IOException | IllegalArgumentException
                | InterruptedException | SecurityException | SchedulerException e) {
            DebugUtils.dumpException(e);
            throw new RuntimeException("General error: " + e.getMessage());
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
     * A method for assigning values to the secretkey and checking the environment variables.
     * @return returns null if everything works. If not it will return a Map
     *     containing an appropriate errormessage and errorsatus.
     * @throws SchedulerException When something goes wrong.
     */
    private void initVariables() throws SchedulerException {
        try {
            readEnvVariables();
            secretKey = SecretRetriever.getAlmaApiKeySecret();
        } catch (IllegalStateException | SecretRetrieverException e) {
            throw new SchedulerException("Failed to initialize variables. ", e);
        }

    }

    /**
     * Stores the systemvariable properties.
     * @return It returnes a boolean indicating that it retrieved the systemvariables.
     */
    public boolean readEnvVariables() {
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
