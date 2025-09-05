package no.unit.aws;

import software.amazon.awssdk.services.sqs.SqsClient;


public abstract class SqsClientFactory {

    public SqsClient create() {
        return createSqsClient();
    }

    protected abstract SqsClient createSqsClient();

}
