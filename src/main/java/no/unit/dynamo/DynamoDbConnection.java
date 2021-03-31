package no.unit.dynamo;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.gson.Gson;

import javax.xml.stream.events.EndDocument;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class DynamoDbConnection {

    public static final String ENDPOINT = "dynamodb.eu-west-1.amazonaws.com";
    public static final String REGION = "eu-west-1";
    public static final String CONTENTS_KEY = "contents";
    public static final String TIME_KEY = ":time";

    public DynamoDbHelperClass dynamoHelper = new DynamoDbHelperClass();
    Gson g = new Gson();

    public Item getItemsFromDynamoDB(String isbnNumber) throws Exception {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.
                        EndpointConfiguration(ENDPOINT, REGION))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        Table contentsTable = dynamoDB.getTable(CONTENTS_KEY);

        GetItemSpec spec = new GetItemSpec().withPrimaryKey("isbn", isbnNumber);

        try {
            System.out.println("Attempting to read the item...");
            Item outcome = contentsTable.getItem(spec);
            System.out.println("GetItem succeeded: " + outcome);
            return outcome;

        }
        catch (Exception e) {
            System.err.println("Unable to read item: " + isbnNumber);
            System.err.println(e.getMessage());
            return null;
        }

    }

    public List<DynamoDbItem> getAllRecordsFromYesterday(String tableColumn) throws Exception {
        List<DynamoDbItem> dynamoDbItemList = new ArrayList<>();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(ENDPOINT, REGION))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        Table table = dynamoDB.getTable(CONTENTS_KEY);

        String yesterday = dynamoHelper.getDateAsString();

        ScanSpec scanSpec = new ScanSpec()
                .withFilterExpression(tableColumn + " > " + TIME_KEY)
                .withValueMap(new ValueMap().withString(TIME_KEY, yesterday));

        try {
            ItemCollection<ScanOutcome> items = table.scan(scanSpec);

            Iterator<Item> iter = items.iterator();
            while (iter.hasNext()) {
                Item item = iter.next();
                String itemJson = item.toJSON();
                DynamoDbItem dynamoItem = g.fromJson(itemJson, DynamoDbItem.class);
                dynamoDbItemList.add(dynamoItem);
            }

        }
        catch (Exception e) {
            System.err.println("Unable to scan the table:");
            System.err.println(e.getMessage());
        }
        return dynamoDbItemList;
    }


}