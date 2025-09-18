package no.unit.alma;

import static no.unit.utils.HttpUtils.nonSuccessful;
import java.util.List;
import no.unit.exceptions.ParsingException;
import no.unit.marc.Reference;
import no.unit.scheduler.UpdateItem;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlmaUpdater {

    private static final Logger logger = LoggerFactory.getLogger(AlmaUpdater.class);

    private static final String ALMA_UPDATE_COMPLETE_FOR_MMS_ID =
        "Completed the update in Alma for post with mms_id: {}";

    private final AlmaClient almaClient;
    private final BibRecordEnricher bibRecordEnricher;

    private int successCounter;

    @JacocoGenerated
    public AlmaUpdater() {
        this(new AlmaClient(), new BibRecordEnricher(new DocumentXmlParser()));
    }

    public AlmaUpdater(AlmaClient almaClient, BibRecordEnricher bibRecordEnricher) {
        this.almaClient = almaClient;
        this.bibRecordEnricher = bibRecordEnricher;
    }

    public void update(List<UpdateItem> updateItems, List<Reference> references)
        throws InterruptedException, ParsingException {

        successCounter = 0;

        for (Reference reference : references) {
            /* 3.1 Get the MMS_ID from the REFERENCE OBJECT. */
            var mmsId = reference.getId();

            /* 3.2 Use the MMS_ID to get a BIB-RECORD from the alma-api. */
            var getResponse = almaClient.getBibRecordFromAlmaWithRetries(mmsId);

            if (nonSuccessful(getResponse)) {
                continue;
            }

            var xmlFromAlma = getResponse.body();

            /* 3.3 Create an XML(String) by updating the existing ALMA xml with all the updateItems. */
            var updatedRecord = bibRecordEnricher.enrich(updateItems, xmlFromAlma);

            /* 4. Push the updated BIB-RECORD back to the alma through a put-request to the api. */
            var putResponse = almaClient.putBibRecordInAlmaWithRetries(mmsId, updatedRecord);

            if (nonSuccessful(putResponse)) {
                continue;
            }

            logger.info(ALMA_UPDATE_COMPLETE_FOR_MMS_ID, mmsId);
            successCounter++;
        }
    }

    public int getSuccessCounter() {
        return successCounter;
    }

}
