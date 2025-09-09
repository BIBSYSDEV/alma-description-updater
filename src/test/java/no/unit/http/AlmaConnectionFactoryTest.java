package no.unit.http;

import static no.unit.alma.Config.ALMA_API_HOST_KEY;
import static no.unit.alma.Config.ALMA_API_KEY;
import static no.unit.alma.Config.ALMA_SRU_HOST_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.doReturn;
import java.net.http.HttpClient;
import no.unit.alma.Config;
import nva.commons.core.Environment;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AlmaConnectionFactoryTest {

    @Mock
    private Environment mockEnv;

    @Mock
    private HttpClientFactory mockHttpClientFactory;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private Config mockConfig;

    @InjectMocks
    private AlmaConnectionFactory almaConnectionFactory;

    @Test
    @SuppressWarnings("resource")
    public void shouldCreateAlmaConnectionFromFactory() {
        MockitoAnnotations.openMocks(this);
        doReturn("alma_host").when(mockEnv).readEnv(ALMA_API_HOST_KEY);
        doReturn("alma_sru_host").when(mockEnv).readEnv(ALMA_SRU_HOST_KEY);
        doReturn("alma_api_key").when(mockEnv).readEnv(ALMA_API_KEY);
        mockConfig = new Config(mockEnv);
        doReturn(mockHttpClient).when(mockHttpClientFactory).createHttpClient();

        almaConnectionFactory = new AlmaConnectionFactory(mockConfig, mockHttpClientFactory);
        var almaConnection = almaConnectionFactory.create();

        assertThat(almaConnection, instanceOf(AlmaConnection.class));
    }

}