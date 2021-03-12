package no.unit.alma;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlParserTest {

    public static final int NUMBER_OF_SUBFIELDS_2 = 2;
    public static final int NUMBER_OF_SUBFIELDS_3 = 3;

    public static final String MOCK_UPDATE_NODE = "/Update_node.xml";
    public static final String FAULTY_XML_FILE = "/Faulty_xml.xml";
    public static final String CORRECT_XML_FILE = "/Mock_xml.xml";
    public static final String UPDATED_XML_FILE = "/Updated_xml.xml";
    public static final String UPDATED_FAULTY_XML_FILE = "/Updated_faulty_xml.xml";

    public static final String MOCK_DESCRIPTION = "This is the description";
    public static final String MOCK_URL = "This/is/the/url";


    /**
     * A helper method that returnes a string from a source.
     * @param file The file/source you want to retrieve the string from.
     * @return A string-value representing the content of the source.
     * @throws Exception when something goes wrong.
     */
    public String setup(String file) throws Exception {
        InputStream stream = XmlParserTest.class.getResourceAsStream(file);
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(reader);
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        return sb.toString();
    }

    /**
     * Helper method that lets you print a Document.
     * @param doc The document you want to print.
     */
    public void printDocument(Document doc) {
        Node topNode = doc.getFirstChild();
        NodeList nodeList = topNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).hasChildNodes()) {
                printChildNodes(nodeList.item(i).getChildNodes());
            } else {
                if (!nodeList.item(i).getTextContent().isBlank()) {
                    System.out.println(nodeList.item(i).getTextContent());
                }
            }
        }
    }

    /**
     * Helper method to the printDocument method.
     * Prints the childnodes in a Document.
     * @param children A nodelist containing the children you wish to print.
     */
    public void printChildNodes(NodeList children) {
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).hasChildNodes()) {
                printChildNodes(children.item(i).getChildNodes());
            } else {
                if (!children.item(i).getTextContent().isBlank()) {
                    System.out.println(children.item(i).getTextContent());
                }
            }

        }
    }

    @Test
    public void testCreatingUpdateNodeWithAndWithoutType() throws Exception {
        DocumentXmlParser parser = new DocumentXmlParser();
        Document doc = parser.create856Node(MOCK_DESCRIPTION, MOCK_URL);
        NodeList datafields = doc.getElementsByTagName("datafield");
        NodeList subfields = datafields.item(0).getChildNodes();
        assertEquals(NUMBER_OF_SUBFIELDS_2, subfields.getLength());
        Document doc2 = parser.create856Node(MOCK_DESCRIPTION, MOCK_URL + ".jpg");
        NodeList datafields2 = doc2.getElementsByTagName("datafield");
        NodeList subfields2 = datafields2.item(0).getChildNodes();
        assertEquals(NUMBER_OF_SUBFIELDS_3, subfields2.getLength());

    }

    @Test
    public void testInsertUpdatedIntoRecord() throws Exception {
        String mockXml = setup(CORRECT_XML_FILE);
        String updatedMockXml = setup(UPDATED_XML_FILE);
        DocumentXmlParser parser = new DocumentXmlParser();
        Document updateDoc = parser.create856Node(MOCK_DESCRIPTION, MOCK_URL);
        Document doc = parser.insertUpdatedIntoRecord(mockXml, updateDoc);
        assertEquals(updatedMockXml, parser.convertDocToString(doc));
    }

    @Test
    public void testDatafieldAtWrongPlaceInXml() throws Exception {
        String faultyMockXml = setup(FAULTY_XML_FILE);
        String updatedFaultyMockXml = setup(UPDATED_FAULTY_XML_FILE);
        DocumentXmlParser parser = new DocumentXmlParser();
        Document updateDoc = parser.create856Node(MOCK_DESCRIPTION, MOCK_URL);
        Document doc = parser.insertUpdatedIntoRecord(faultyMockXml, updateDoc);
        assertEquals(updatedFaultyMockXml, parser.convertDocToString(doc));
    }

    @Test
    public void testConvertDocToString() throws Exception {
        String mockXml = setup(CORRECT_XML_FILE);
        DocumentXmlParser parser = new DocumentXmlParser();
        Document updateDoc = parser.create856Node(MOCK_DESCRIPTION, MOCK_URL);
        Document doc = parser.insertUpdatedIntoRecord(mockXml, updateDoc);
        assertNotNull(parser.convertDocToString(doc));
    }

    @Test
    public void testCreate856Node() throws Exception {
        String theNode = setup(MOCK_UPDATE_NODE);
        DocumentXmlParser xmlParser = new DocumentXmlParser();
        Document doc = xmlParser.create856Node("Beskrivelse fra forlaget (kort)", "http://innhold.bibsys.no/bilde/forside/?size=mini&id=LITE_150088182.jpg");
        assertEquals(theNode, xmlParser.convertDocToString(doc));
    }

    @Test
    public void testDuplicateLinkAndDescription() throws Exception {
        String mockXml = setup(CORRECT_XML_FILE);
        DocumentXmlParser xmlParser = new DocumentXmlParser();
        assertTrue(xmlParser.alreadyExists("Beskrivelse fra forlaget (kort)", "http://content.bibsys.no/content/?type=descr_publ_brief&isbn=8210053418", mockXml));
    }

}
