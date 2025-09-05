package no.unit.http;

import static no.unit.alma.Config.ALMA_API_HOST_KEY;
import static no.unit.alma.Config.ALMA_API_KEY;
import static no.unit.alma.Config.ALMA_SRU_HOST_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import java.net.http.HttpClient;
import no.unit.alma.Config;
import nva.commons.core.Environment;
import org.junit.jupiter.api.Test;

public class AlmaConnectionFactoryTest {

    @Test
    @SuppressWarnings("resource")
    public void shouldCreateAlmaConnectionFromFactory() {
        var mockEnv = mock(Environment.class);
        doReturn("alma_host").when(mockEnv).readEnv(ALMA_API_HOST_KEY);
        doReturn("alma_sru_host").when(mockEnv).readEnv(ALMA_SRU_HOST_KEY);
        doReturn("alma_api_key").when(mockEnv).readEnv(ALMA_API_KEY);
        var mockConfig = new Config(mockEnv);
        var mockHttpClientFactory = mock(HttpClientFactory.class);
        var mockHttpClient = mock(HttpClient.class);
        doReturn(mockHttpClient).when(mockHttpClientFactory).createHttpClient();

        ConnectionFactory connectionFactory = new AlmaConnectionFactory(mockConfig, mockHttpClientFactory);
        var almaConnection = connectionFactory.create();

        assertThat(almaConnection, instanceOf(AlmaConnection.class));
    }

}