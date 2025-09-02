package no.unit.aws;

import software.amazon.awssdk.services.sqs.SqsClient;


@FunctionalInterface
public interface SqsClientFactory {

    SqsClient createSqsClient();

}
