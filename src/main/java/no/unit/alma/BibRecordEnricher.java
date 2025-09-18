package no.unit.alma;

import java.util.List;
import no.unit.exceptions.ParsingException;
import no.unit.scheduler.UpdateItem;
import org.w3c.dom.Document;

public class BibRecordEnricher {

    private final DocumentXmlParser xmlParser;

    public BibRecordEnricher(DocumentXmlParser xmlParser) {
        this.xmlParser = xmlParser;
    }

    /**
     * Create an XML(String) by updating the existing ALMA xml with all the UpdateItems.
     * @param updateItems A list of UpdateItems.
     * @param xmlFromAlma A String in the shape of an XML the data is retrieved from ALMA.
     * @return The same XML data that was entered now with added fields (either 856 or 956).
     * @throws ParsingException When something goes wrong.
     */
    public String enrich(List<UpdateItem> updateItems, String xmlFromAlma) throws ParsingException {
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

}
