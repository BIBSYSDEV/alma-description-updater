package no.unit.dynamo;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.gson.Gson;
import no.unit.exceptions.DynamoDbException;
import nva.commons.utils.Environment;

import java.util.*;


public class DynamoDbConnection {

    public static final String ENDPOINT = "dynamodb.eu-west-1.amazonaws.com";
    public static final String REGION = "eu-west-1";
    public static final String CONTENTS_KEY = "contents";
    public static final String TIME_KEY = ":time";

    public DynamoDbHelperClass dynamoHelper;

    public DynamoDbConnection(){ dynamoHelper = new DynamoDbHelperClass(); }

    public DynamoDbConnection(Environment envhandler){ dynamoHelper = new DynamoDbHelperClass(envhandler); }

    Gson g = new Gson();

    public List<DynamoDbItem> getAllRecordsFromYesterday(String tableColumn) throws DynamoDbException {
        List<DynamoDbItem> dynamoDbItemList = new ArrayList<>();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(ENDPOINT, REGION))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        Table table = dynamoDB.getTable(CONTENTS_KEY);

        String yesterday = dynamoHelper.getYesterDaysDate();

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
            throw new DynamoDbException("Unable to scan the table:", e);
        }
        return dynamoDbItemList;
    }


}