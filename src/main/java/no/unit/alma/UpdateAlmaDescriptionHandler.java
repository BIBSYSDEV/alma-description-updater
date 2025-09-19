package no.unit.alma;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import no.unit.exceptions.HttpOperationFailedException;
import no.unit.exceptions.ParsingException;
import no.unit.exceptions.SchedulerException;
import no.unit.marc.Reference;
import no.unit.scheduler.SchedulerHelper;
import no.unit.scheduler.UpdateItem;
import no.unit.utils.DebugUtils;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UpdateAlmaDescriptionHandler implements RequestHandler<SQSEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(UpdateAlmaDescriptionHandler.class);

    private static final String WRITING_TO_DLQ = "No answer from SRU for isbn: {} . Writing to DLQ";
    private static final String FOUND_POSTS_FOR_THE_ISBN = "Found {} different posts for the isbn: {}";
    public static final String ONE_OR_MORE_MMS_IDS_FROM_ISBN_FAILED =
        "1 or more mms_id's did not go through with isbn: ";
    public static final String GENERAL_ERROR = "General error: ";
    public static final String ERROR_PROCESSING_INPUT_EVENT = "Error while processing input event. ";
    private static final String ALMA_PARTIAL_SUCCESS = "Alma succeeded only {} of {} times";

    private final transient ReferenceListCreator referenceListCreator;
    private final transient AlmaUpdater almaUpdater;
    private final transient SchedulerHelper schedulerHelper;
    private final transient IsbnConverter isbnConverter;

    @SuppressWarnings("unused")
    @JacocoGenerated
    public UpdateAlmaDescriptionHandler() {
        this(new ReferenceListCreator(),
             new AlmaUpdater(),
             new SchedulerHelper(),
             new IsbnConverter());
    }

    public UpdateAlmaDescriptionHandler(ReferenceListCreator referenceListCreator,
                                        AlmaUpdater almaUpdater,
                                        SchedulerHelper schedulerHelper,
                                        IsbnConverter isbnConverter) {
        this.referenceListCreator = referenceListCreator;
        this.almaUpdater = almaUpdater;
        this.schedulerHelper = schedulerHelper;
        this.isbnConverter = isbnConverter;
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
    public Void handleRequest(final SQSEvent event, Context context) {
        /* 1. Create an UpdateItem LIST from the input. */
        var updateItems = getUpdateItems(event);

        if (updateItems.isEmpty()) {
            return null;
        }

        try {
            /* Step 2. Get a REFERENCE LIST from alma-sru through a lambda. */
            var isbn = firstElementIsbn(updateItems);
            var convertedIsbn = isbnConverter.convertIsbn(isbn);

            var referenceList = new ArrayList<Reference>();
            referenceList.addAll(referenceListCreator.create(isbn));
            referenceList.addAll(referenceListCreator.create(convertedIsbn));

            if (referenceList.isEmpty()) {
                logger.info(WRITING_TO_DLQ, convertedIsbn);
                schedulerHelper.writeToDLQ(event.getRecords().getFirst().getBody());
                return null;
            }

            logger.info(FOUND_POSTS_FOR_THE_ISBN, referenceList.size(), isbn);

            /* 3. Loop through the LIST. */
            almaUpdater.update(updateItems, referenceList);

            if (almaUpdater.getSuccessCounter() < referenceList.size()) {
                logger.error(ALMA_PARTIAL_SUCCESS, almaUpdater.getSuccessCounter(), referenceList.size());
                throw new HttpOperationFailedException(ONE_OR_MORE_MMS_IDS_FROM_ISBN_FAILED + isbn);
            }
        } catch (ParsingException | IOException | IllegalArgumentException
                 | InterruptedException | SecurityException | SchedulerException e) {
            DebugUtils.dumpException(e);
            throw new RuntimeException(GENERAL_ERROR + e.getMessage());
        }

        return null;
    }

    private List<UpdateItem> getUpdateItems(SQSEvent event) {
        try {
            return schedulerHelper.splitEventIntoUpdateItems(event.getRecords().getFirst().getBody());
        } catch (Exception e) {
            throw new RuntimeException(ERROR_PROCESSING_INPUT_EVENT + e.getMessage());
        }
    }

    private String firstElementIsbn(List<UpdateItem> updateItems) {
        return updateItems.getFirst().getIsbn();
    }

}
