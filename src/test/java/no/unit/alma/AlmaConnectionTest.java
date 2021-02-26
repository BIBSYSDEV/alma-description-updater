package no.unit.alma;

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class AlmaConnectionTest {

    private static final String MMS_ID = "991325803064702201";

    public static final String CORRECT_XML_FILE = "/Mock_xml.xml";

    @Test
    public void testAlmaConnectionSendGet() throws Exception{
        AlmaConnection almaCon = new AlmaConnection();
        SecretRetriever secretRetriever = new SecretRetriever();
        String secretKey = secretRetriever.getSecret();
        HttpResponse<String> response = almaCon.sendGet(MMS_ID, secretKey);
        System.out.println(response.body());
        assertEquals(200, response.statusCode());
    }

    @Test
    public void testAlmaConnectionSendPut() throws Exception{
        InputStream stream = XmlParserTest.class.getResourceAsStream(CORRECT_XML_FILE);
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
        String xml = parser.convertDocToString(doc);

        AlmaConnection almaCon = new AlmaConnection();
        SecretRetriever secretRetriever = new SecretRetriever();
        String secretKey = secretRetriever.getSecret();
        HttpResponse<String> response = almaCon.sendPut(MMS_ID, secretKey, xml);
        System.out.println(response.statusCode());
        assertEquals(200, response.statusCode());
    }
}