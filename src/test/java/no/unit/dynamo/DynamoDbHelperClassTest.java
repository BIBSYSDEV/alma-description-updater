package no.unit.dynamo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import no.unit.alma.XmlParserTest;
import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamoDbHelperClassTest {

    private static final String CREATED_KEY = "created";
    private static final String MODIFIED_KEY = "modified";
    private static final String IMAGE_URL_KEY = "image-url-com";
    private static final String CONTENT_URL_KEY = "content-url-com";
    private static final String ISBN = "9788205377547";
    private static final String IMAGE_SIZE = "small";
    private static final String CONTENT_TYPE = "description_short";
    private static final String NEWVERSION = "/newVersion.JSON";
    private static final String OLDVERSION = "/oldVersion.JSON";
    private static final String RETURNVERSION = "/returnVersion.JSON";

    Environment mockEnv;
    DynamoDbHelperClass mockDynamoDbHelper;

    public void printList(List<?> theList) {
        Iterator<?> iter = theList.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            System.out.println(item.toString());
        }
    }

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

    private void initEnv() {
        when(mockEnv.readEnv("STANDARD_IMAGE_URL")).thenReturn(IMAGE_URL_KEY);
        when(mockEnv.readEnv("STANDARD_CONTENT_URL")).thenReturn(CONTENT_URL_KEY);
    }

    /**
     * Sets up a mock environment for use when testing.
     */
    @BeforeEach
    public void init() {
        mockEnv = mock(Environment.class);
        initEnv();
        mockDynamoDbHelper = new DynamoDbHelperClass(mockEnv);
    }

    @Test
    void generateImageLinkTest() throws Exception {
        UpdatePayload payload = mockDynamoDbHelper.createImageLink(IMAGE_SIZE, ISBN);
        String expectedLink = String.format(IMAGE_URL_KEY + IMAGE_SIZE + "/%s/%s/%s.jpg", 7, 4, ISBN);
        assertEquals(expectedLink, payload.getLink());
    }

    @Test
    void generateContentLinkTest() throws Exception {
        UpdatePayload payload = mockDynamoDbHelper.createContentLink(CONTENT_TYPE, ISBN);
        String expectedLink = String.format(CONTENT_URL_KEY + ISBN + "?type=" + CONTENT_TYPE.toUpperCase());
        assertEquals(expectedLink, payload.getLink());
    }

    @Test
    void extractDiffsTest() throws Exception {
        Gson gson = new Gson();
        ObjectMapper objectMapper = new ObjectMapper();
        String oldVersion = setup(OLDVERSION);
        String newVersion = setup(NEWVERSION);
        String returnVersion = setup(RETURNVERSION);
        DynamoDbItem oldItem = objectMapper.readValue(oldVersion, DynamoDbItem.class);
        DynamoDbItem newItem = objectMapper.readValue(newVersion, DynamoDbItem.class);
        DynamoDbItem returnItem = objectMapper.readValue(returnVersion, DynamoDbItem.class);
        DynamoDbItem theItem = mockDynamoDbHelper.extractDiffs(newItem, oldItem);
        assertEquals(returnItem.toString(), theItem.toString());
    }

}