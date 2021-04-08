package no.unit.dynamo;

import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamoDbConnectionTest {

    public static String CREATED_KEY = "created";
    public static String MODIFIED_KEY = "modified";

    Environment mockEnv;
    DynamoDbConnection mockDynamoConnection;

    public void printList(List<?> theList) {
        Iterator<?> iter = theList.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            System.out.println(item.toString());
        }
    }

    private void initEnv() {
        when(mockEnv.readEnv("STANDARD_IMAGE_URL")).thenReturn("image-url-com");
        when(mockEnv.readEnv("STANDARD_CONTENT_URL")).thenReturn("content-url-com");
    }

    /**
     * Sets up a mock environment for use when testing.
     */
    @BeforeEach
    public void init() {
        mockEnv = mock(Environment.class);
        initEnv();
        mockDynamoConnection = new DynamoDbConnection(mockEnv);
    }


    @Test
    void getAllRecordsFromYesterdayTest() throws Exception {
        List<DynamoDbItem> dynamoDbItemList = mockDynamoConnection.getAllRecordsFromYesterday(CREATED_KEY);
        printList(dynamoDbItemList);
    }

}