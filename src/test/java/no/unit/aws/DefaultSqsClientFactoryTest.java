package no.unit.aws;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DefaultSqsClientFactoryTest {

    @Test
    public void shouldCreateSqsClientFromFactory() {
        SqsClientFactory factory = new DefaultSqsClientFactory();

        assertThat(factory.create(), instanceOf(SqsClient.class));
    }

}
