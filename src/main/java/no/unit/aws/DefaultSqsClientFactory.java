package no.unit.aws;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DefaultSqsClientFactory extends SqsClientFactory {

    @Override
    protected SqsClient createSqsClient() {
        return SqsClient.builder()
                   .region(Region.EU_WEST_1)
                   .build();
    }

}
