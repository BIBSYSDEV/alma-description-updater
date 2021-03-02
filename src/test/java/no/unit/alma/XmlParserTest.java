package no.unit.alma;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.net.http.HttpResponse;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlParserTest {

    public static final int NUMBER_OF_SUBFIELDS_2 = 2;
    public static final int NUMBER_OF_SUBFIELDS_3 = 3;

    public static final String MMS_ID = "991325803064702201";

    public static final String FAULTY_XML_FILE = "/Faulty_xml.xml";
    public static final String CORRECT_XML_FILE = "/Mock_xml.xml";

    public static final String MOCK_XML ="<bib>"
                    +"<record xmlns='http://www.loc.gov/MARC21/slim'>"
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
                    +"</record>"
            +"</bib>";

    public void printDocument(Document doc){
        Node topNode = doc.getFirstChild();
        NodeList nodeList = topNode.getChildNodes();
        for(int i = 0; i < nodeList.getLength(); i++){
            if(nodeList.item(i).hasChildNodes()){
                printChildNodes(nodeList.item(i).getChildNodes());
            }else{
                if(!nodeList.item(i).getTextContent().isBlank()) {
                    System.out.println(nodeList.item(i).getTextContent());
                }
            }
        }
    }

    public void printChildNodes(NodeList children){
        for (int i = 0; i < children.getLength(); i++){
            if(children.item(i).hasChildNodes()){
                printChildNodes(children.item(i).getChildNodes());
            }else{
                if(!children.item(i).getTextContent().isBlank()) {
                    System.out.println(children.item(i).getTextContent());
                }
            }

        }
    }

    @Test
    public void testXmlInsertion() throws Exception{
        XmlParser parser = new XmlParser();
        Document doc = parser.create856Node("This is the description", "This is the url");
        NodeList datafields = doc.getElementsByTagName("datafield");
        NodeList subfields = datafields.item(0).getChildNodes();
        assertEquals(NUMBER_OF_SUBFIELDS_2, subfields.getLength());
        Document doc2 = parser.create856Node("This is the description", "This/is/the/url.jpg");
        NodeList datafields2 = doc2.getElementsByTagName("datafield");
        NodeList subfields2 = datafields2.item(0).getChildNodes();
        assertEquals(NUMBER_OF_SUBFIELDS_3, subfields2.getLength());

    }

    @Test
    public void testInsertUpdatedIntoRecord() throws Exception{
        XmlParser parser = new XmlParser();
        Document updateDoc = parser.create856Node("This is the description", "This is the url");
        Document doc = parser.insertUpdatedIntoRecord(MOCK_XML, updateDoc);
        printDocument(doc);
    }

    @Test
    public void testInsertUpdatedRecordIntoAlmaRecord() throws Exception{
        AlmaConnection almaCon = new AlmaConnection();
        SecretRetriever secretRetriever = new SecretRetriever();
        String secretKey = secretRetriever.getSecret();
        HttpResponse<String> result = almaCon.sendGet(MMS_ID, secretKey);
        XmlParser parser = new XmlParser();
        Document updateDoc = parser.create856Node("This is the description", "This is the url");
        Document doc = parser.insertUpdatedIntoRecord(result.body(), updateDoc);
        printDocument(doc);
    }

    @Test
    public void testDatafieldAtWrongPlaceInXml() throws Exception{
        InputStream stream = XmlParserTest.class.getResourceAsStream(FAULTY_XML_FILE);
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(reader);
        String line;
        StringBuilder sb = new StringBuilder();
        while((line=br.readLine())!= null){
            sb.append(line.trim());
        }
        XmlParser parser = new XmlParser();
        Document updateDoc = parser.create856Node("This is a test", "This is a test url");
        Document doc = parser.insertUpdatedIntoRecord(sb.toString(), updateDoc);
        printDocument(doc);
    }

    @Test
    public void testConvertDocToString() throws Exception{
        InputStream stream = XmlParserTest.class.getResourceAsStream(FAULTY_XML_FILE);
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(reader);
        String line;
        StringBuilder sb = new StringBuilder();
        while((line=br.readLine())!= null){
            sb.append(line.trim());
        }
        XmlParser parser = new XmlParser();
        Document updateDoc = parser.create856Node("This is a test", "This is a test url");
        Document doc = parser.insertUpdatedIntoRecord(sb.toString(), updateDoc);
        System.out.println(parser.convertDocToString(doc));
    }

    @Test
    public void testCreate856Node() throws Exception{
        XmlParser xmlParser = new XmlParser();
        Document doc = xmlParser.create856Node("Beskrivelse fra forlaget (kort)", "http://innhold.bibsys.no/bilde/forside/?size=mini&id=LITE_150088182.jpg");
        printDocument(doc);
    }

}
