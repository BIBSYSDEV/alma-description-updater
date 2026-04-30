package no.unit.http;

import no.unit.alma.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlmaProxyConnectionTest {

    public static final String ALMA_SRU_HOST = "https://api.example.com/";
    public static final String ISBN = "9788210053412";

    @Mock
    private Config mockConfig;

    @Mock
    private HttpClientFactory mockHttpClientFactory;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    private AlmaProxyConnection almaProxyConnection;

    @BeforeEach
    @SuppressWarnings("resource")
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockConfig.getAlmaSruHost()).thenReturn(ALMA_SRU_HOST);
        when(mockHttpClientFactory.create()).thenReturn(mockHttpClient);

        almaProxyConnection = new AlmaProxyConnection(mockConfig, mockHttpClientFactory);
    }

    @Test
    void shouldSendHttpRequestWithCorrectUri() throws IOException, InterruptedException {
        final var expectedUri = ALMA_SRU_HOST + ISBN;
        var expectedResponseBody = "response body";

        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(mockHttpResponse);
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);

        var actualResponse = almaProxyConnection.sendGet(ISBN);

        var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()));

        var capturedRequest = requestCaptor.getValue();

        assertEquals(URI.create(expectedUri), capturedRequest.uri());
        assertEquals(expectedResponseBody, actualResponse.body());
    }

    @Test
    void shouldThrowIOExceptionWhenHttpClientFails() throws IOException, InterruptedException {
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenThrow(new IOException("Test IOException"));

        var exception = assertThrows(IOException.class, () -> almaProxyConnection.sendGet(ISBN));

        assertEquals("Test IOException", exception.getMessage());
    }

    @Test
    void shouldThrowInterruptedExceptionWhenHttpClientIsInterrupted() throws IOException, InterruptedException {
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenThrow(new InterruptedException("Test InterruptedException"));

        var exception = assertThrows(InterruptedException.class, () -> almaProxyConnection.sendGet(ISBN));

        assertEquals("Test InterruptedException", exception.getMessage());
    }
}
