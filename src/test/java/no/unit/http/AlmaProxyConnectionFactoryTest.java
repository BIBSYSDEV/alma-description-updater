package no.unit.http;

import no.unit.alma.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AlmaProxyConnectionFactoryTest {

    @Mock
    private Config mockConfig;

    @Mock
    private HttpClientFactory mockHttpClientFactory;

    private GetConnectionFactory almaProxyConnectionFactory;

    @BeforeEach
    @SuppressWarnings("resource")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        almaProxyConnectionFactory = new AlmaProxyConnectionFactory(mockConfig, mockHttpClientFactory);
    }

    @Test
    void shouldCreateInstanceWithCustomParameters() {
        var almaProxyConnection = almaProxyConnectionFactory.create();

        assertNotNull(almaProxyConnection);
        assertThat(almaProxyConnection, instanceOf(AlmaProxyConnection.class));
    }

}