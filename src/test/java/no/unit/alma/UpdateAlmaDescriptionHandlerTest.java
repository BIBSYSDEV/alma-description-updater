package no.unit.alma;

import com.google.gson.reflect.TypeToken;
import no.unit.alma.Config;
import no.unit.alma.GatewayResponse;
import no.unit.alma.UpdateAlmaDescriptionHandler;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import java.io.StringReader;
import no.unit.marc.Reference;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class UpdateAlmaDescriptionHandlerTest {

    public static final String MOCK_UPDATE_HOST = "ALMA_SRU_HOST";
    public static final String MOCK_ISBN = "9788290625974";
    public static final String EXPECTED_ID = "991325803064702201";
    public static final String MOCK_DESCRIPTION = "This is a test";
    public static final String MOCK_URL = "thisIsATest.jpg";

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
                    +"<subfield code='u'>http://innhold.bibsys.no/bilde/forside/?size=mini&id=LITE_150088182.jpg</subfield>"
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
    public void testConnectionToAlmaSru(){
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(UpdateAlmaDescriptionHandler.ISBN_KEY, MOCK_ISBN);
        Map<String, Object> event = new HashMap<>();
        event.put(UpdateAlmaDescriptionHandler.QUERY_STRING_PARAMETERS_KEY, queryParameters);

        final UpdateAlmaDescriptionHandler updateAlmaDescriptionHandler = new UpdateAlmaDescriptionHandler();

        final GatewayResponse gatewayResponse = updateAlmaDescriptionHandler.handleRequest(event, null);
        String result = gatewayResponse.getBody();
        System.out.println(result);
    }

    @Test
    public void testIdMatchBasedOnIsbn(){
        final Config instance = Config.getInstance();
        instance.setAlmaSruEndpoint(MOCK_UPDATE_HOST);

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(UpdateAlmaDescriptionHandler.ISBN_KEY, MOCK_ISBN);
        Map<String, Object> event = new HashMap<>();
        event.put(UpdateAlmaDescriptionHandler.QUERY_STRING_PARAMETERS_KEY, queryParameters);

        final UpdateAlmaDescriptionHandler updateAlmaDescriptionHandler = new UpdateAlmaDescriptionHandler();

        final GatewayResponse gatewayResponse = updateAlmaDescriptionHandler.handleRequest(event, null);
        String result = gatewayResponse.getBody();

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        Type listOfMyClassObject = new TypeToken<List<Reference>>() {}.getType();
        List<Reference> reference = gson.fromJson(result, listOfMyClassObject);
        assertEquals(EXPECTED_ID, reference.get(0).getId());
    }

    @Test
    public void testDuplicateLenkeAndDescription() throws Exception{
        String newXML = MOCK_XML.substring(MOCK_XML.indexOf("<recordData>") + 12,  MOCK_XML.lastIndexOf("</recordData>"));
        newXML = "<?xml version='1.0' encoding='UTF-8'?>" + newXML;
        newXML = newXML.replace("&", "&amp;");
        System.out.println(newXML);
        XmlParser xmlParser = new XmlParser();
        assertTrue(xmlParser.alreadyExists("Beskrivelse fra forlaget (kort)", "http://innhold.bibsys.no/bilde/forside/?size=mini&amp;id=LITE_150088182.jpg".replace("&amp;", "&"), newXML));
    }

    @Test
    public void testTheCompleteLoop() throws Exception{
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(UpdateAlmaDescriptionHandler.ISBN_KEY, MOCK_ISBN);
        queryParameters.put(UpdateAlmaDescriptionHandler.DESCRPTION_KEY, MOCK_DESCRIPTION);
        queryParameters.put(UpdateAlmaDescriptionHandler.URL_KEY, MOCK_URL);
        Map<String, Object> event = new HashMap<>();
        event.put(UpdateAlmaDescriptionHandler.QUERY_STRING_PARAMETERS_KEY, queryParameters);
        UpdateAlmaDescriptionHandler updateHandler = new UpdateAlmaDescriptionHandler();
        GatewayResponse response = updateHandler.handleRequest(event, null);
        System.out.println(response.getBody());
        assertEquals(200, response.getStatusCode());
    }

}