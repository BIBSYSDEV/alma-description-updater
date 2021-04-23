package no.unit.alma;


import com.google.gson.Gson;
import no.unit.scheduler.UpdateItem;
import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.regions.Region;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateAlmaDescriptionHandlerTest {

    public static final String CORRECT_XML_FILE = "/Mock_xml.xml";
    public static final String UPDATED_XML_FILE = "/UpdatedGroupXml.xml";
    public static final String MOCKEVENT_FILE = "/MockEvent.JSON";


    Environment mockEnv;
    UpdateAlmaDescriptionHandler mockedHandler;

    private void initEnv() {
        when(mockEnv.readEnv("ALLOWED_ORIGIN")).thenReturn("Allow-origins");
        when(mockEnv.readEnv("ALMA_SRU_HOST")).thenReturn("Alma-sru-dot-com");
        when(mockEnv.readEnv("ALMA_API_HOST")).thenReturn("Alma-api-dpot-com");
    }

    /**
     * Sets up a mock environment for use when testing.
     */
    @BeforeEach
    public void init() {
        mockEnv = mock(Environment.class);
        initEnv();
        mockedHandler = new UpdateAlmaDescriptionHandler(mockEnv);
    }

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

    @Test
    public void testCheckProperties() throws Exception {
        assertTrue(mockedHandler.readEnvVariables());
    }

}