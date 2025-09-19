package no.unit.alma;

import static no.unit.utils.FileUtils.setup;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import com.google.gson.Gson;
import java.util.ArrayList;
import no.unit.scheduler.UpdateItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BibRecordEnricherTest {

    private static final String CORRECT_XML_FILE = "/Mock_xml.xml";
    private static final String UPDATED_XML_FILE = "/UpdatedGroupXml.xml";

    private static BibRecordEnricher bibRecordEnricher;

    @BeforeAll
    static void setUp() {
        bibRecordEnricher = new BibRecordEnricher();
    }

    @Test
    public void shouldUpdateBibRecordAndSkipTheUpdatesThatAreEqual() throws Exception {
        var gson = new Gson();
        var mockXml = setup(CORRECT_XML_FILE);
        var item1String = "{isbn: 1234, link: 1234_small_1234.jpg, specifiedMaterial: Small_coverFoto}";
        var item2String = "{isbn: 1234, link: 1234_large_1234.jpg, specifiedMaterial: Large_coverFoto}";
        var item1 = gson.fromJson(item1String, UpdateItem.class);
        var item2 = gson.fromJson(item2String, UpdateItem.class);
        var updateItemList = new ArrayList<UpdateItem>();
        updateItemList.add(item1);
        updateItemList.add(item1);
        updateItemList.add(item2);
        var mockUpdatedXml = setup(UPDATED_XML_FILE);

        var updatedXml = bibRecordEnricher.enrich(updateItemList, mockXml);

        assertThat(updatedXml, equalTo(mockUpdatedXml));
    }

}
