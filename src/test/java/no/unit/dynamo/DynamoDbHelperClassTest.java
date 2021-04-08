package no.unit.dynamo;

import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    Environment mockEnv;
    DynamoDbHelperClass mockDynamoDbHelper;

    public void printList(List<?> theList) {
        Iterator<?> iter = theList.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            System.out.println(item.toString());
        }
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
    void createLinksTest() throws Exception {
        DynamoDbConnection dbConnection = new DynamoDbConnection(mockEnv);
        List<DynamoDbItem> dynamoDbItemList = dbConnection.getAllRecordsFromYesterday(MODIFIED_KEY);
        List<UpdatePayload> payloadList = mockDynamoDbHelper.createLinks(dynamoDbItemList);
        printList(payloadList);
    }

}