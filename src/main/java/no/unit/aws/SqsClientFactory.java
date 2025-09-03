package no.unit.aws;

import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.sqs.SqsClient;


public abstract class SqsClientFactory {

    @JacocoGenerated
    public SqsClient create() {
        return createSqsClient();
    }

    protected abstract SqsClient createSqsClient();

}
