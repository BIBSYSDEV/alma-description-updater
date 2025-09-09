package no.unit.alma;


import com.google.gson.Gson;
import java.net.http.HttpClient;
import no.unit.http.AlmaConnection;
import no.unit.http.ConnectionFactory;
import no.unit.http.HttpClientFactory;
import no.unit.scheduler.UpdateItem;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateAlmaDescriptionHandlerTest {

    public static final String CORRECT_XML_FILE = "/Mock_xml.xml";
    public static final String UPDATED_XML_FILE = "/UpdatedGroupXml.xml";


    @Mock
    private Environment mockEnv;

    @Mock
    private HttpClientFactory mockHttpClientFactory;

    @Mock
    private ConnectionFactory mockConnectionFactory;

    @InjectMocks
    private Config config;

    @InjectMocks
    private AlmaConnection mockConnection;

    @InjectMocks
    private AlmaClient almaClient;

    private UpdateAlmaDescriptionHandler mockedHandler;

    private void initEnv() {
        when(mockEnv.readEnv("ALLOWED_ORIGIN")).thenReturn("Allow-origins");
        when(mockEnv.readEnv("ALMA_SRU_HOST")).thenReturn("Alma-sru-dot-com");
        when(mockEnv.readEnv("ALMA_API_HOST")).thenReturn("Alma-api-dpot-com");
    }

    /**
     * Sets up a mock environment for use when testing.
     */
    @BeforeEach
    @SuppressWarnings("resource")
    public void init() {
        MockitoAnnotations.openMocks(this);
        initEnv();
        var mockHttpClient = mock(HttpClient.class);
        doReturn(mockHttpClient).when(mockHttpClientFactory).create();
        doReturn(mockConnection).when(mockConnectionFactory).create();

        mockedHandler = new UpdateAlmaDescriptionHandler(config, almaClient, new IsbnConverter());
    }

    /**
     * A helper method that returnes a string from a source.
     * @param file The file/source you want to retrieve the string from.
     * @return A string-value representing the content of the source.
     * @throws Exception when something goes wrong.
     */
    public String setup(String file) throws Exception {
        InputStream stream = DocumentXmlParserTest.class.getResourceAsStream(file);
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
    public void updateBibRecordTest() throws Exception {
        Gson g = new Gson();
        String mockXml = setup(CORRECT_XML_FILE);
        String mockUpdatedXml = setup(UPDATED_XML_FILE);
        String item1String = "{isbn: 1234, link: 1234_small_1234.jpg, specifiedMaterial: Small_coverFoto}";
        String item2String = "{isbn: 1234, link: 1234_large_1234.jpg, specifiedMaterial: Large_coverFoto}";
        UpdateItem item1 = g.fromJson(item1String, UpdateItem.class);
        UpdateItem item2 = g.fromJson(item2String, UpdateItem.class);
        List<UpdateItem> updateItemList = new ArrayList<>();
        updateItemList.add(item1);
        updateItemList.add(item2);
        String updatedXml = mockedHandler.updateBibRecord(updateItemList, mockXml);
        assertEquals(mockUpdatedXml, updatedXml);
    }


}