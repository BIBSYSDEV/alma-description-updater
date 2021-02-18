package no.unit.alma;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlParserTest {

    public static final int NUMBER_OF_SUBFIELDS_2 = 2;
    public static final int NUMBER_OF_SUBFIELDS_3 = 3;

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

    public void printDocument(Document doc){
        Node topNode = doc.getFirstChild();
        NodeList nodeList = topNode.getChildNodes();
        for(int i = 0; i < nodeList.getLength(); i++){
            if(nodeList.item(i).hasChildNodes()){
                NodeList nodeChildren = nodeList.item(i).getChildNodes();
                for (int j = 0; j < nodeChildren.getLength(); j++){
                    System.out.println(nodeChildren.item(j).getTextContent());
                }
            }else{
                System.out.println(nodeList.item(i).getTextContent());
            }
        }
    }

    @Test
    public void testXmlInsertion() throws Exception{
        XmlParser parser = new XmlParser();
        Document doc = parser.create856Node("This is the description", "This is the url", null);
        NodeList datafields = doc.getElementsByTagName("datafield");
        NodeList subfields = datafields.item(0).getChildNodes();
        System.out.println(subfields.item(0).getTextContent());
        System.out.println(subfields.item(1).getTextContent());
        assertEquals(NUMBER_OF_SUBFIELDS_2, subfields.getLength());
        Document doc2 = parser.create856Node("This is the description", "This is the url", "This is det type");
        NodeList datafields2 = doc2.getElementsByTagName("datafield");
        NodeList subfields2 = datafields2.item(0).getChildNodes();
        assertEquals(NUMBER_OF_SUBFIELDS_3, subfields2.getLength());
        //TODO Use this next time
        System.out.println(datafields.item(0).getAttributes().getNamedItem("tag"));

    }

    @Test
    public void testInsertUpdatedIntoRecord() throws Exception{
        XmlParser parser = new XmlParser();
        Document updateDoc = parser.create856Node("This is the description", "This is the url", null);
        Document doc = parser.insertUpdatedIntoRecord(MOCK_XML, updateDoc);
        printDocument(doc);
    }
}
