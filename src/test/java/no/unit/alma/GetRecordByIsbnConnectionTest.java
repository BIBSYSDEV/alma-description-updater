package no.unit.alma;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GetRecordByIsbnConnectionTest {

    public static final String MOCK_XML = "/Mock_xml.xml";

    @Test
    public void testConnect() throws IOException {
        GetRecordByIsbnConnection isbnConnection = new GetRecordByIsbnConnection();
        final URL localFileUrl = GetRecordByIsbnConnectionTest.class.getResource(MOCK_XML);
        final InputStreamReader streamReader = isbnConnection.connect(localFileUrl);
        assertNotNull(streamReader);
        streamReader.close();
    }
}