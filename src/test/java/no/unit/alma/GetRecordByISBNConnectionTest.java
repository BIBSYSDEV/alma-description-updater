package no.unit.alma;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class GetRecordByISBNConnectionTest {

    public static String MOCK_XML = "/Mock_xml.xml";

    @Test
    public void testConnect() throws IOException {
        GetRecordByISBNConnection isbnConnection = new GetRecordByISBNConnection();
        final URL localFileUrl = GetRecordByISBNConnectionTest.class.getResource(MOCK_XML);
        final InputStreamReader streamReader = isbnConnection.connect(localFileUrl);
        assertNotNull(streamReader);
        streamReader.close();
    }
}