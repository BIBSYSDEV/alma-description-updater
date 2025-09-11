package no.unit.alma;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import no.unit.exceptions.ParsingException;
import no.unit.exceptions.SchedulerException;
import no.unit.http.AlmaProxyConnectionFactory;
import no.unit.http.GetConnection;
import no.unit.http.GetConnectionFactory;
import no.unit.marc.Reference;
import no.unit.scheduler.SchedulerHelper;
import no.unit.scheduler.UpdateItem;
import no.unit.utils.DebugUtils;
import nva.commons.core.JacocoGenerated;
import org.w3c.dom.Document;
import software.amazon.awssdk.http.HttpStatusCode;


public class UpdateAlmaDescriptionHandler implements RequestHandler<SQSEvent, Void> {

    private final transient AlmaClient almaClient;
    private final transient SchedulerHelper schedulerHelper;
    private final transient DocumentXmlParser xmlParser;
    private final transient IsbnConverter isbnConverter;
    private final transient GetConnection almaSruConnection;

    @SuppressWarnings("unused")
    @JacocoGenerated
    public UpdateAlmaDescriptionHandler() {
        this(new AlmaClient(),
             new SchedulerHelper(),
             new DocumentXmlParser(),
             new IsbnConverter(),
             new AlmaProxyConnectionFactory());
    }

    public UpdateAlmaDescriptionHandler(AlmaClient almaClient,
                                        SchedulerHelper schedulerHelper,
                                        DocumentXmlParser xmlParser,
                                        IsbnConverter isbnConverter,
                                        GetConnectionFactory almaSruConnectionFactory) {
        this.almaClient = almaClient;
        this.schedulerHelper = schedulerHelper;
        this.xmlParser = xmlParser;
        this.isbnConverter = isbnConverter;
        this.almaSruConnection = almaSruConnectionFactory.create();
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
    @SuppressWarnings("PMD.CognitiveComplexity")
    public Void handleRequest(final SQSEvent event, Context context) {
        /* 1. Create an UpdateItem LIST from the input. */
        List<UpdateItem> updateItems;
        try {
            updateItems = schedulerHelper.splitEventIntoUpdateItems(event.getRecords().getFirst().getBody());
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
            List<Reference> referenceList = getReferenceListByIsbn(updateItems.getFirst().getIsbn());
            if (referenceList == null || referenceList.isEmpty()) {
                System.out.println("No answer from SRU for isbn: " + updateItems.getFirst().getIsbn());
                referenceList = getReferenceListByIsbn(isbnConverter.convertIsbn(updateItems.getFirst().getIsbn()));
                if (referenceList == null || referenceList.isEmpty()) {
                    System.out.println("No answer from SRU for isbn: "
                                       + isbnConverter.convertIsbn(updateItems.getFirst().getIsbn())
                                       + ". Writing to DLQ");
                    schedulerHelper.writeToDLQ(event.getRecords().getFirst().getBody());
                    return null;
                }
            } else {
                List<Reference> convertedIsbnList =
                    getReferenceListByIsbn(isbnConverter.convertIsbn(updateItems.getFirst().getIsbn()));
                if (convertedIsbnList == null || convertedIsbnList.isEmpty()) {
                    System.out.println("No answer from SRU for isbn: "
                            + isbnConverter.convertIsbn(updateItems.getFirst().getIsbn()));
                } else {
                    referenceList.addAll(convertedIsbnList);
                }
            }

            HttpResponse<String> almaResponse = null;
            HttpResponse<String> response = null;
            int sucessCounter = 0;
            /* 3. Loop through the LIST. */
            System.out.println("Found " + referenceList.size() + " different posts for the isbn: "
                    + updateItems.getFirst().getIsbn());
            for (Reference reference : referenceList) {
                /* 3.1 Get the MMS_ID from the REFERENCE OBJECT. */
                String mmsId = reference.getId();

                /* 3.2 Use the MMS_ID to get a BIB-RECORD from the alma-api. */
                almaResponse = almaClient.getBibRecordFromAlmaWithRetries(mmsId);

                if (almaResponse == null || almaResponse.statusCode() != HttpStatusCode.OK) {
                    continue;
                }

                String xmlFromAlma = almaResponse.body();

                /* 3.3 Create an XML(String) by updating the existing ALMA xml with all the updateItems. */
                String updatedRecord = updateBibRecord(updateItems, xmlFromAlma);

                /* 4. Push the updated BIB-RECORD back to the alma through a put-request to the api. */
                response = almaClient.putBibRecordInAlmaWithRetries(mmsId, updatedRecord);

                if (response == null || response.statusCode() != HttpStatusCode.OK) {
                    continue;
                }
                System.out.println("Completed the update in Alma for post with mms_id: " + mmsId);
                sucessCounter++;
            }
            if (sucessCounter < referenceList.size()) {
                if (almaResponse == null) {
                    throw new RuntimeException("1 or more mms_id's did not go through with mms_id: "
                            + updateItems.getFirst().getIsbn()
                            + System.lineSeparator() + "Get failed");
                }
                if (response == null) {
                    throw new RuntimeException("1 or more mms_id's did not go through with mms_id: "
                            + updateItems.getFirst().getIsbn()
                            + System.lineSeparator() + "Get response " + almaResponse.body());
                }
                throw new RuntimeException("1 or more mms_id's did not go through with mms_id: "
                        + updateItems.getFirst().getIsbn()
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
     * Retrieve a list of reference objects based on the isbn you enter.
     * @param isbn The isbn you wish to retrieve refrence objects based on.
     * @return A list of reference objects matching the isbn, this list will usually contain only one reference object.
     * @throws IOException when something goes wrong
     * @throws InterruptedException when something goes wrong
     */
    private List<Reference> getReferenceListByIsbn(String isbn) throws IOException, InterruptedException {
        var almaSruResponse = fetchFromAlmaSruProxy(isbn);
        if (almaSruResponse.statusCode() != HttpStatusCode.OK) {
            return Collections.emptyList();
        }

        return createReferenceListFromAlmaSruProxyResponse(almaSruResponse.body());
    }

    private HttpResponse<String> fetchFromAlmaSruProxy(String isbn) throws IOException, InterruptedException {
        return almaSruConnection.sendGet(isbn);
    }

    private List<Reference> createReferenceListFromAlmaSruProxyResponse(String response) {
        var gsonBuilder = new GsonBuilder();
        var gson = gsonBuilder.create();
        var listOfMyClassObject = new TypeToken<List<Reference>>() {}.getType();

        return gson.fromJson(response, listOfMyClassObject);
    }

}
