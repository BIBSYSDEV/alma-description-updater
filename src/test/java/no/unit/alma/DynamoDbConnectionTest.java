package no.unit.alma;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.google.gson.Gson;
import no.unit.dynamo.DynamoDbConnection;
import no.unit.dynamo.DynamoDbHelperClass;
import no.unit.dynamo.DynamoDbItem;
import no.unit.dynamo.UpdatePayload;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DynamoDbConnectionTest {

    public static String CREATED_KEY = "created";
    public static String MODIFIED_KEY = "modified";

    public void printList(List<?> theList) {
        Iterator<?> iter = theList.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            System.out.println(item.toString());
        }
    }

    @Test
    void getItemsFromDynamoDBTest() throws Exception {
        DynamoDbConnection dbConnection = new DynamoDbConnection();
        dbConnection.getItemsFromDynamoDB("22323");
        assertTrue(true);
    }

    @Test
    void getAllRecordsFromYesterdayTest() throws Exception {
        DynamoDbConnection dbConnection = new DynamoDbConnection();
        List<DynamoDbItem> dynamoDbItemList = dbConnection.getAllRecordsFromYesterday(CREATED_KEY);
        printList(dynamoDbItemList);
        assertTrue(true);
    }

    @Test
    void createDBItemTest() throws Exception {
        Gson g = new Gson();
        DynamoDbConnection dbConnection = new DynamoDbConnection();
        Item theItem = dbConnection.getItemsFromDynamoDB("22323");
        String itemJson = theItem.toJSON();
        DynamoDbItem dynamoItem = g.fromJson(itemJson, DynamoDbItem.class);
        assertEquals("22323", dynamoItem.getIsbn());
    }

    @Test
    void generateImageLinkTest() throws Exception {
        DynamoDbHelperClass dynamoDbHelper = new DynamoDbHelperClass();
        UpdatePayload payload = dynamoDbHelper.createImageLink("small", "9788205377547");
        System.out.println(payload.getLink());
        System.out.println(payload.getSpecifiedMaterial());
    }

    @Test
    void generateContentLinkTest() throws Exception {
        DynamoDbHelperClass dynamoDbHelper = new DynamoDbHelperClass();
        UpdatePayload payload = dynamoDbHelper.createContentLink("description_short", "9788210053412");
        System.out.println(payload.getLink());
        System.out.println(payload.getSpecifiedMaterial());
    }

    @Test
    void createLinksTest() throws Exception {
        DynamoDbConnection dbConnection = new DynamoDbConnection();
        DynamoDbHelperClass dynamoDbHelper = new DynamoDbHelperClass();
        List<DynamoDbItem> dynamoDbItemList = dbConnection.getAllRecordsFromYesterday(CREATED_KEY);
        List<UpdatePayload> payloadList = dynamoDbHelper.createLinks(dynamoDbItemList);
        printList(payloadList);
    }
}