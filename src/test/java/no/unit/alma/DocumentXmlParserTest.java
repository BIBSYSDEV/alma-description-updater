package no.unit.alma;

import no.unit.exceptions.ParsingException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.util.Objects.isNull;
import static no.unit.alma.DocumentXmlParser.MARC_TAG_856;
import static no.unit.alma.DocumentXmlParser.MARC_TAG_956;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocumentXmlParserTest {

    public static final int NUMBER_OF_SUBFIELDS_2 = 2;
    public static final int NUMBER_OF_SUBFIELDS_3 = 3;

    public static final String MOCK_UPDATE_NODE = "/Update_node.xml";
    public static final String MOCK_UPDATE_NODE_MARC_956 = "/update_node_marc_956.xml";
    public static final String FAULTY_XML_FILE = "/Faulty_xml.xml";
    public static final String CORRECT_XML_FILE = "/Mock_xml.xml";
    public static final String UPDATED_XML_FILE = "/Updated_xml.xml";
    public static final String UPDATED_FAULTY_XML_FILE = "/Updated_faulty_xml.xml";
    public static final String MOCK_ELECTRONIC_XML_FILE = "/Mock_Electronic_xml.xml";

    public static final String MOCK_DESCRIPTION = "This is the description";
    public static final String MOCK_URL = "This/is/the/url";
    public static final String SHORT_DESCRIPTION = "Beskrivelse fra forlaget (kort)";
    public static final String SOME_URL = "http://content.bibsys.no/content/?type=descr_publ_brief&isbn=8210053418";
    public static final String ERROR_WHEN_CHECKING_ALREADY_EXISTS =
        "Caught an error while checking if the update fields already exists";
    public static final String ERROR_WHILE_CONVERTING_TO_DOCUMENT = "Caught an error while converting to document";
    public static final String HOBBIT_TITLE_TAG = "<title>Hobbiten : Smaugs ødemark i bilder</title>";

    /**
     * A helper method that returnes a string from a source.
     * @param file The file/source you want to retrieve the string from.
     * @return A string-value representing the content of the source.
     * @throws Exception when something goes wrong.
     */
    public String setup(String file) throws Exception {
        InputStream stream = DocumentXmlParserTest.class.getResourceAsStream(file);
        if (isNull(stream)) {
            throw new RuntimeException("Could not load xml file " + file);
        }
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(reader);
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        return sb.toString();
    }

    @Test
    public void testCreatingUpdateNodeWithAndWithoutType() throws Exception {
        DocumentXmlParser parser = new DocumentXmlParser();
        Document doc = parser.createNode(MOCK_DESCRIPTION, MOCK_URL, MARC_TAG_856);
        NodeList datafields = doc.getElementsByTagName("datafield");
        NodeList subfields = datafields.item(0).getChildNodes();
        assertEquals(NUMBER_OF_SUBFIELDS_2, subfields.getLength());
        Document doc2 = parser.createNode(MOCK_DESCRIPTION, MOCK_URL + ".jpg", MARC_TAG_856);
        NodeList datafields2 = doc2.getElementsByTagName("datafield");
        NodeList subfields2 = datafields2.item(0).getChildNodes();
        assertEquals(NUMBER_OF_SUBFIELDS_3, subfields2.getLength());

    }

    @Test
    public void testInsertUpdatedIntoRecord() throws Exception {
        String mockXml = setup(CORRECT_XML_FILE);
        String updatedMockXml = setup(UPDATED_XML_FILE);
        DocumentXmlParser parser = new DocumentXmlParser();
        Document updateDoc = parser.createNode(MOCK_DESCRIPTION, MOCK_URL, MARC_TAG_856);
        Document doc = parser.insertUpdatedIntoRecord(mockXml, updateDoc, MARC_TAG_856);
        assertEquals(updatedMockXml, parser.convertDocToString(doc));
    }

    @Test
    public void testGetTagNumber() throws Exception {
        DocumentXmlParser parser = new DocumentXmlParser();
        Document doc = parser.createNode(MOCK_DESCRIPTION, MOCK_URL, MARC_TAG_856);
        NodeList datafields = doc.getElementsByTagName("datafield");
        assertEquals(MARC_TAG_856, parser.getTagNumber(datafields.item(0)));
    }

    @Test
    public void testGetSubfieldCode() throws Exception {
        DocumentXmlParser parser = new DocumentXmlParser();
        Document doc = parser.createNode(MOCK_DESCRIPTION, MOCK_URL, MARC_TAG_856);
        NodeList datafields = doc.getElementsByTagName("datafield");
        assertEquals(DocumentXmlParser.MARC_CODE_3, parser
                .getSubfieldCode(datafields.item(0).getChildNodes().item(0)));
        assertEquals(DocumentXmlParser.MARC_CODE_U, parser
                .getSubfieldCode(datafields.item(0).getChildNodes().item(1)));
    }

    @Test
    public void testDatafieldAtWrongPlaceInXml() throws Exception {
        String faultyMockXml = setup(FAULTY_XML_FILE);
        String updatedFaultyMockXml = setup(UPDATED_FAULTY_XML_FILE);
        DocumentXmlParser parser = new DocumentXmlParser();
        Document updateDoc = parser.createNode(MOCK_DESCRIPTION, MOCK_URL, MARC_TAG_856);
        Document doc = parser.insertUpdatedIntoRecord(faultyMockXml, updateDoc, MARC_TAG_856);
        assertEquals(updatedFaultyMockXml, parser.convertDocToString(doc));
    }

    @Test
    public void testConvertDocToString() throws Exception {
        String mockXml = setup(CORRECT_XML_FILE);
        DocumentXmlParser parser = new DocumentXmlParser();
        Document updateDoc = parser.createNode(MOCK_DESCRIPTION, MOCK_URL, MARC_TAG_856);
        Document doc = parser.insertUpdatedIntoRecord(mockXml, updateDoc, MARC_TAG_856);
        var actual = parser.convertDocToString(doc);
        assertNotNull(actual);
        assertThat(actual, containsString(HOBBIT_TITLE_TAG));
    }

    @Test
    public void testCreate856Node() throws Exception {
        String theNode = setup(MOCK_UPDATE_NODE);
        DocumentXmlParser xmlParser = new DocumentXmlParser();
        Document doc = xmlParser.createNode(SHORT_DESCRIPTION,
                                            "http://innhold.bibsys.no/bilde/forside/?size=mini&id=LITE_150088182"
                                               + ".jpg", MARC_TAG_856);
        assertEquals(theNode, xmlParser.convertDocToString(doc));
    }

    @Test
    public void testDuplicateLinkAndDescription() throws Exception {
        String mockXml = setup(CORRECT_XML_FILE);
        DocumentXmlParser xmlParser = new DocumentXmlParser();
        assertTrue(xmlParser.alreadyExists(SHORT_DESCRIPTION,
                                           "http://content.bibsys.no/content/?type=descr_publ_brief&isbn"
                                              + "=8210053418", mockXml, MARC_TAG_856));
    }

    @Test
    public void testDetermineElectronicOrPrint() throws Exception {
        String mockPrint = setup(CORRECT_XML_FILE);
        String mockElectronic = setup(MOCK_ELECTRONIC_XML_FILE);
        DocumentXmlParser xmlParser = new DocumentXmlParser();
        assertEquals(MARC_TAG_856, xmlParser.determineElectronicOrPrint(mockPrint));
        assertEquals(MARC_TAG_956, xmlParser.determineElectronicOrPrint(mockElectronic));
    }

    @Test
    public void shouldCreate956NodeWhenAppropriate() throws Exception {
        var expected = setup(MOCK_UPDATE_NODE_MARC_956);
        var xmlParser = new DocumentXmlParser();
        var doc = xmlParser
                      .createNode(SHORT_DESCRIPTION,
                                  "http://innhold.bibsys.no/bilde/forside/?size=mini&id=LITE_150088182.jpg",
                                  MARC_TAG_956);

        var actual = xmlParser.convertDocToString(doc);

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void shouldCreate956NodeEvenWhenHavingTypeMp3() throws Exception {
        var expected = setup(MOCK_UPDATE_NODE_MARC_956)
                           .replace(".jpg", ".mp3")
                           .replace("image/jpeg", "audio/mpeg");
        var xmlParser = new DocumentXmlParser();
        var doc = xmlParser
                      .createNode(SHORT_DESCRIPTION,
                                  "http://innhold.bibsys.no/bilde/forside/?size=mini&id=LITE_150088182.mp3",
                                  MARC_TAG_956);

        var actual = xmlParser.convertDocToString(doc);

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void shouldReturnDocumentWithoutUpdatingWhenTagNumberLessThanMarcTag() throws Exception {
        var mockXml = setup(CORRECT_XML_FILE);
        var parser = new DocumentXmlParser();
        var updateDoc = parser.createNode(MOCK_DESCRIPTION, MOCK_URL, MARC_TAG_856);
        var doc = parser.insertUpdatedIntoRecord(mockXml, updateDoc, 10000);

        var actual = parser.convertDocToString(doc);

        assertThat(actual, containsString(HOBBIT_TITLE_TAG));
    }

    @Test
    public void shouldHandleExceptionWhenCheckingAlreadyExistsOnInvalidXml() {
        var mockXml = "not a xml file";
        var parser = new DocumentXmlParser();

        var exception = assertThrows(ParsingException.class, () -> parser.alreadyExists(SHORT_DESCRIPTION,
                                                                                        SOME_URL,
                                                                                        mockXml,
                                                                                        MARC_TAG_856));

        assertThat(exception.getMessage(), containsString(ERROR_WHEN_CHECKING_ALREADY_EXISTS));
    }

    @Test
    public void shouldHandleExceptionWhenConvertingInvalidXmlStringToDocument() {
        var mockXml = "not a xml file";
        var parser = new DocumentXmlParser();

        var exception = assertThrows(ParsingException.class, () -> parser.asDocument(mockXml));

        assertThat(exception.getMessage(), containsString(ERROR_WHILE_CONVERTING_TO_DOCUMENT));
    }

}
