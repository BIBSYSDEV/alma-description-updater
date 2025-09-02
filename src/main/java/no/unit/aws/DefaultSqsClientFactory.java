package no.unit.aws;

import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DefaultSqsClientFactory implements SqsClientFactory {

    @Override
    @JacocoGenerated
    public SqsClient createSqsClient() {
        return SqsClient.builder()
                   .region(Region.EU_WEST_1)
                   .build();
    }

}
