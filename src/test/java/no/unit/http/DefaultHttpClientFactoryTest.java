package no.unit.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.net.http.HttpClient.Version;
import org.junit.jupiter.api.Test;

public class DefaultHttpClientFactoryTest {

    @Test
    void shouldCreateHttpClientFromFactory() {
        var factory = new DefaultHttpClientFactory();
        try (var httpClient = factory.create()) {
            assertNotNull(httpClient);
            assertThat(httpClient.version(), equalTo(Version.HTTP_2));
        }
    }

}
