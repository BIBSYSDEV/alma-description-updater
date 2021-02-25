package no.unit.alma;

import org.marc4j.MarcXmlReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;


public class XmlParser {

    public static final String EMPTY_STRING = "";
    public static final String MARC_TAG_001 = "001";
    public static final String MARC_TAG_856 = "856";
    public static final char MARC_CODE_U = 'u';
    public static final char MARC_CODE_3 = '3';
    public static final String MARC_PREFIX = "marc:";
    public static final String DATAFIELD = "datafield";
    public static final String NODE_TEMPLATE ="<datafield tag='856' ind1='4' ind2='2'>"
            + "<subfield code='3'>1</subfield>"
            + "<subfield code='u'>2</subfield>"
            + "<subfield code='q'>3</subfield>"
            + "</datafield>";


    public XmlParser(){
    }

    /**
     *
     * @param xml A xml in the form of a String
     * @return The mms_id of the xml(bib-record)
     * @throws TransformerException
     */
    @SuppressWarnings("PMD.NcssCount")
    public String extractMms_id(String xml) throws TransformerException{
        Record record = asMarcRecord(asDocument(xml));
        List<ControlField> controlFieldList = record.getControlFields();
        for (ControlField controlField : controlFieldList) {
            String controlFieldTag = controlField.getTag();
            if (controlFieldTag.equals(MARC_TAG_001)) {
                return controlField.getData();
            }
        }
        return null;
    }

    /**
     *
     * @param description The description we want to popluate the node with
     * @param url The url we want to popluate the node with
     * @param descType The type we want to popluate the node with. Set to null unless the resource is a picture, if it is this should be 'image/jpg'
     * @return A document populated with the fields set from the params
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public Document create856Node(String description, String url, String descType) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(NODE_TEMPLATE));

        Document doc = db.parse(is);

        NodeList datafields = doc.getElementsByTagName(DATAFIELD);
        NodeList subfields = datafields.item(0).getChildNodes();

        subfields.item(0).setTextContent(description);
        subfields.item(1).setTextContent(url);

        if(descType != null) {
            subfields.item(2).setTextContent(descType);
        } else {
            datafields.item(0).removeChild(subfields.item(2));
        }

        return doc;
    }

    /**
     * @param xml The xml that we want to insert the extra info into
     * @param update The update document that we want to insert into the xml
     * @return Document. The updated document with the added info
     * This method assumes that the xml starts with an outer node (in this case <bib>)
     * the outer node contains several metadata nodes before the last node which is the <record>
     */
    public Document insertUpdatedIntoRecord(String xml, Document update){
        Document doc = asDocument(xml);
        Node updateNode = doc.importNode(update.getFirstChild(), true);
        NodeList datafields = doc.getElementsByTagName(DATAFIELD);
        int i;
        for(i=0; i<datafields.getLength(); i++){
            Node datafield = datafields.item(i);
            if(getTagNumber(datafield) >= 856) {
                try{
                    doc.getFirstChild().getLastChild().insertBefore(updateNode, datafield);
                    return doc;
                }catch(Exception e){
                    /** We dont want to handle this exception
                     * its just a failsafe in case of an out-of-place datafield-tag
                     * in this case we just skip it
                     */
                    continue;
                }
            }
        }
        System.out.println(doc.getChildNodes().getLength());
        System.out.println(doc.getFirstChild().getChildNodes().getLength());
        System.out.println(doc.getFirstChild().getLastChild().getChildNodes().getLength());
        doc.getFirstChild().getLastChild().appendChild(updateNode);
        return doc;
    }

    /**
     *
     * @param node the node from which we want to extract the tag number
     * @return int the tag number
     */
    public int getTagNumber(Node node){
        String tagString = node.getAttributes().getNamedItem("tag").toString();
        String tagNumberString = tagString.substring(5, tagString.length()-1);
        int tagInt = Integer.parseInt(tagNumberString);
        return tagInt;
    }

    /**
     *
     * @param description The description we want to check if exists
     * @param url The url we want to check if exists
     * @param xml The xml we want to check if url and description already exists inn.
     * @return True if both description and url exists on the same 856 node, false if not
     * @throws TransformerException
     */
    public boolean alreadyExists(String description, String url, String xml) throws TransformerException{
        Record record = asMarcRecord(asDocument(xml));
        List<DataField> dataFieldList = record.getDataFields();
        for (DataField dataField : dataFieldList) {
            Subfield subField_U;
            Subfield subField_3;
            String dataFieldTag = dataField.getTag();
            if (dataFieldTag.equals(MARC_TAG_856)) {
                subField_U = dataField.getSubfield(MARC_CODE_U);
                subField_3 = dataField.getSubfield(MARC_CODE_3);
                if(subField_U != null && subField_3 != null) {
                    System.out.println(subField_3.getData() + "    " + subField_U.getData());
                    if(subField_U.getData().equals(url) && subField_3.getData().equals(description)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private Record asMarcRecord(Document doc) throws TransformerException {
        ByteArrayOutputStream outputStream = removeStylesheet(doc);
        return new MarcXmlReader(new ByteArrayInputStream(outputStream.toByteArray())).next();
    }


    private ByteArrayOutputStream removeStylesheet(Document result) throws TransformerException {
        Source source = new DOMSource(result);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Result outputTarget = new StreamResult(outputStream);
        TransformerFactory.newInstance().newTransformer().transform(source, outputTarget);
        return outputStream;
    }

    public Document asDocument(String sruxml) {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            String removedMarcInSruXml = sruxml.replace(MARC_PREFIX, EMPTY_STRING);
            String fixedAmpersandInSruXML = removedMarcInSruXml.replace("&", "&amp;");
            InputSource is = new InputSource(new StringReader(fixedAmpersandInSruXML));
            document = builder.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println("Something went wrong during parsing of sruResponse. " + e.getMessage());
        }
        return document;
    }

}
