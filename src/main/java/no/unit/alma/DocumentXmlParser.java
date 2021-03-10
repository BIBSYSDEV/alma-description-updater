package no.unit.alma;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;


public class DocumentXmlParser {

    public static final String UPDATE_NODE_ERROR_MESSAGE = "Caught and error while creating the updateNode";
    public static final String ALREADY_EXISTS_ERROR_MESSAGE =
            "Caught an error while checking if the update fields already exists";
    public static final String CONVERTING_TO_DOC_ERROR_MESSAGE = "Caught an error while converting to document";
    public static final String CONVERTING_TO_STRING_ERROR_MESSAGE = "Caught an error while converting to String";

    public static final String EMPTY_STRING = "";
    public static final int MARC_TAG_856 = 856;
    public static final char MARC_CODE_U = 'u';
    public static final char MARC_CODE_3 = '3';
    public static final String MARC_PREFIX = "marc:";
    public static final String DATAFIELD = "datafield";
    public static final String NODE_TEMPLATE = "<datafield tag='856' ind1='4' ind2='2'>"
            + "<subfield code='3'>1</subfield>"
            + "<subfield code='u'>2</subfield>"
            + "<subfield code='q'>image/jpeg</subfield>"
            + "</datafield>";

    /**
     * This method creates a Document in the shape of a marc-856 node.
     * @param description The description we want to popluate the node with.
     * @param url The url we want to popluate the node with.
     * @return A document populated with the fields set from the params.
     * @throws ParsingException when something goes wrong.
     */
    public Document create856Node(String description, String url) throws ParsingException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(NODE_TEMPLATE));

            Document doc = db.parse(is);

            NodeList datafields = doc.getElementsByTagName(DATAFIELD);
            NodeList subfields = datafields.item(0).getChildNodes();

            subfields.item(0).setTextContent(description);
            subfields.item(1).setTextContent(url);

            if (!url.endsWith(".jpg")) {
                datafields.item(0).removeChild(subfields.item(2));
            }
            return doc;

        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new ParsingException(UPDATE_NODE_ERROR_MESSAGE, e);
        }
    }

    /**
     * Inserts the 'update-node' into the xml string and returns the xml as a document.
     * This method assumes that the xml starts with an outer node (in this case 'bib').
     * The outer node contains several metadata nodes before the last node which is the 'record'.
     * @param xml The xml that we want to insert the extra info into.
     * @param update The update document that we want to insert into the xml.
     * @return Document. The updated document with the added info.
     * @throws ParsingException when something goes wrong.
     */
    public Document insertUpdatedIntoRecord(String xml, Document update) throws ParsingException {
        Document doc = asDocument(xml);
        Node updateNode = doc.importNode(update.getFirstChild(), true);
        NodeList datafields = doc.getElementsByTagName(DATAFIELD);
        int i;
        for (i = 0; i < datafields.getLength(); i++) {
            Node datafield = datafields.item(i);
            if (getTagNumber(datafield) >= MARC_TAG_856) {
                try {
                    doc.getFirstChild().getLastChild().insertBefore(updateNode, datafield);
                    return doc;
                } catch (Exception e) {
                    /* We dont want to handle this exception
                     * its just a failsafe in case of an out-of-place datafield-tag
                     * in this case we just skip it
                     */
                    continue;
                }
            }
        }

        doc.getFirstChild().getLastChild().appendChild(updateNode);
        return doc;
    }

    /**
     * Extracts the tag-number from a documentnode.
     * @param node the node from which we want to extract the tag number
     * @return int the tag number
     */
    public int getTagNumber(Node node) {
        String tagString = node.getAttributes().getNamedItem("tag").toString();
        String tagNumberString = tagString.substring(5, tagString.length() - 1);
        int tagInt = Integer.parseInt(tagNumberString);
        return tagInt;
    }

    /**
     * Extracts the subfiel code from a document node.
     * @param node the node containing the code.
     * @return the char the code consists of.
     */
    public char getSubfieldCode(Node node) {
        String codeString = node.getAttributes().getNamedItem("code").toString();
        char code = codeString.charAt(codeString.length() - 2);
        return code;
    }

    /**
     * Checks whether or not the xml already contains the update.
     * @param description The description we want to check if exists.
     * @param url The url we want to check if exists.
     * @param xml The xml we want to check if url and description already exists inn.
     * @return True if both description and url exists on the same 856 node, false if not.
     * @throws ParsingException when something goes wrong.
     */
    public boolean alreadyExists(String description, String url, String xml) throws ParsingException {
        try {
            boolean descriptionMatches = false;
            boolean urlMatches = false;
            Document doc = asDocument(xml);
            NodeList nodeList = doc.getElementsByTagName(DATAFIELD);
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (getTagNumber(nodeList.item(i)) == MARC_TAG_856) {
                    NodeList children = nodeList.item(i).getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        if (getSubfieldCode(children.item(j)) == MARC_CODE_3
                                && children.item(j).getTextContent().trim().equals(description.trim())) {
                            descriptionMatches = true;
                        }
                        if (getSubfieldCode(children.item(j)) == MARC_CODE_U
                                && children.item(j).getTextContent().trim().equals(url.trim())) {
                            urlMatches = true;
                        }
                    }
                    if (descriptionMatches && urlMatches) {
                        return true;
                    }
                }
                descriptionMatches = false;
                urlMatches = false;
            }
            return false;

        } catch (ParsingException e) {
            throw new ParsingException(ALREADY_EXISTS_ERROR_MESSAGE, e);
        }
    }

    /**
     * Converts a document to a one-line string.
     * @param doc the document you want to convert.
     * @return The document in shape of a string.
     * @throws ParsingException when something goes wrong.
     */
    public String convertDocToString(Document doc) throws ParsingException {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
            return output;
        } catch (TransformerException e) {
            throw new ParsingException(CONVERTING_TO_STRING_ERROR_MESSAGE, e);
        }

    }

    /**
     * Converts a string to a document.
     * @param sruxml the string you want to convert.
     * @return the string in shape of a Document.
     * @throws ParsingException when something goes wrong.
     */
    public Document asDocument(String sruxml) throws ParsingException {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            String removedMarcInSruXml = sruxml.replace(MARC_PREFIX, EMPTY_STRING);

            InputSource is = new InputSource(new StringReader(removedMarcInSruXml));
            document = builder.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ParsingException(CONVERTING_TO_DOC_ERROR_MESSAGE, e);
        }
        return document;
    }

}
