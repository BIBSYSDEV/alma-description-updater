package no.unit.alma;

import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import no.unit.marc.Reference;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class UpdateAlmaDescriptionHandlerTest {

    public static final String MOCK_UPDATE_HOST = "ALMA_SRU_HOST";
    public static final String MOCK_ISBN = "9788210053412";
    public static final String EXPECTED_ID = "991325803064702201";
    public static final String MOCK_DESCRIPTION = "Beskrivelse fra forlaget (kort)";
    public static final String MOCK_URL = "http://content.bibsys.no/content/?type=descr_publ_brief&isbn=8210053418";

    public static final String MOCK_XML =
            "<record xmlns='http://www.loc.gov/MARC21/slim'>"
            +"<leader>01044cam a2200301 c 4500</leader>"
                +"<controlfield tag='001'>991325803064702201</controlfield>"
                +"<controlfield tag='005'>20160622160726.0</controlfield>"
                +"<controlfield tag='007'>ta</controlfield>"
                +"<controlfield tag='008'>141124s2013    no#||||j||||||000|0|nob|^</controlfield>"
                  +"<datafield tag='015' ind1=' ' ind2=' '>"
                    +"<subfield code='a'>1337755</subfield>"
                    +"<subfield code='2'>nbf</subfield>"
                  +"</datafield>"
                  +"<datafield tag='020' ind1=' ' ind2=' '>"
                    +"<subfield code='a'>9788210053412</subfield>"
                    +"<subfield code='q'>ib.</subfield>"
                    +"<subfield code='c'>Nkr 249.00</subfield>"
                  +"</datafield>"
                  +"<datafield tag='035' ind1=' ' ind2=' '>"
                    +"<subfield code='a'>132580306-47bibsys_network</subfield>"
                  +"</datafield>"
                  +"<datafield tag='035' ind1=' ' ind2=' '>"
                    +"<subfield code='a'>(NO-TrBIB)132580306</subfield>"
                  +"</datafield>"
                  +"<datafield tag='035' ind1=' ' ind2=' '>"
                    +"<subfield code='a'>(NO-OsBA)0370957</subfield>"
                  +"</datafield>"
                  +"<datafield tag='856' ind1='4' ind2='2'>"
                    +"<subfield code='3'>Beskrivelse fra forlaget (kort)</subfield>"
                    +"<subfield code='u'>http://innhold.bibsys.no/bilde/forside/?size=mini&amp;id=LITE_150088182.jpg</subfield>"
                  +"</datafield>"
                +"<datafield tag='856' ind1='4' ind2='2'>"
                    +"<subfield code='3'>Beskrivelse fra forlaget (Lang)</subfield>"
                    +"<subfield code='u'>http://innhold.bibsysdsds</subfield>"
                +"</datafield>"
                  +"<datafield tag='913' ind1=' ' ind2=' '>"
                    +"<subfield code='a'>Norbok</subfield>"
                    +"<subfield code='b'>NB</subfield>"
                  +"</datafield>"
                +"</record>";


    @Test
    public void testIdMatchBasedOnIsbn() throws Exception{
        final UpdateAlmaDescriptionHandler updateAlmaDescriptionHandler = new UpdateAlmaDescriptionHandler();
        List<Reference> reference = updateAlmaDescriptionHandler.getReferenceListByIsbn(MOCK_ISBN);
        assertEquals(EXPECTED_ID, reference.get(0).getId());
    }


    @Test
    public void testDuplicateLenkeAndDescription() throws Exception{
        String newXML = MOCK_XML;
        newXML = "<?xml version='1.0' encoding='UTF-8'?>" + newXML;
        XmlParser xmlParser = new XmlParser();
        assertTrue(xmlParser.alreadyExists("Beskrivelse fra forlaget (kort)", "http://innhold.bibsys.no/bilde/forside/?size=mini&id=LITE_150088182.jpg", newXML));
    }

}