package no.unit.alma;

import no.unit.http.AlmaConnection;
import no.unit.http.HttpClientFactory;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static no.unit.alma.Config.ALMA_API_HOST_KEY;
import static no.unit.alma.Config.ALMA_API_KEY;
import static no.unit.alma.Config.ALMA_SRU_HOST_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlmaConnectionTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @Mock
    private HttpClientFactory mockHttpClientFactory;

    @Mock
    private Environment mockEnv;

    @Mock
    private Config mockConfig;

    @InjectMocks
    private AlmaConnection almaConnection;

    @BeforeEach
    @SuppressWarnings("resource")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        doReturn("http://mock-alma-api-host/").when(mockEnv).readEnv(ALMA_API_HOST_KEY);
        doReturn("http://mock-alma-sru-host/").when(mockEnv).readEnv(ALMA_SRU_HOST_KEY);
        doReturn("mock-api-key").when(mockEnv).readEnv(ALMA_API_KEY);
        mockConfig = new Config(mockEnv);
        doReturn(mockHttpClient).when(mockHttpClientFactory).create();

        almaConnection = new AlmaConnection(mockConfig, mockHttpClientFactory);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnStatusOkWithResponseBodyOnSuccessfulGetRequest() throws IOException, InterruptedException {
        var mmsId = "123456";
        var expectedResponseBody = "<response>Success</response>";

        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockHttpResponse);

        var response = almaConnection.sendGet(mmsId);

        assertEquals(200, response.statusCode());
        assertEquals(expectedResponseBody, response.body());

        var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        var capturedRequest = requestCaptor.getValue();
        assertEquals("http://mock-alma-api-host/123456", capturedRequest.uri().toString());
        assertEquals("apikey mock-api-key", capturedRequest.headers()
                                                .firstValue("Authorization")
                                                .orElse(""));
    }

    @Test
    @SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
    void shouldReturnStatusOkWithResponseBodyOnSuccessfulPutRequest() throws IOException, InterruptedException {
        var mmsId = "123456";
        var xmlBody = "<xml>Updated Content</xml>";
        var expectedResponseBody = "<response>Success</response>";

        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockHttpResponse);

        var response = almaConnection.sendPut(mmsId, xmlBody);

        assertEquals(200, response.statusCode());
        assertEquals(expectedResponseBody, response.body());

        var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        var capturedRequest = requestCaptor.getValue();
        assertEquals("http://mock-alma-api-host/123456", capturedRequest.uri().toString());
        assertEquals("apikey mock-api-key", capturedRequest.headers()
                                                .firstValue("Authorization")
                                                .orElse(""));
        assertEquals("application/xml", capturedRequest.headers()
                                            .firstValue("Content-Type")
                                            .orElse(""));
        assertTrue(capturedRequest.bodyPublisher().get().contentLength() > 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldThrowIoExceptionOnError() throws IOException, InterruptedException {
        var mmsId = "123456";
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("Mock IOException"));

        var exception = assertThrows(IOException.class, () -> almaConnection.sendGet(mmsId));
        assertEquals("Mock IOException", exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnInternalServerErrorStatusCodeWhenSendPutFail() throws IOException, InterruptedException {
        var mmsId = "123456";
        var xmlBody = "<xml>Updated Content</xml>";

        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockHttpResponse);

        var response = almaConnection.sendPut(mmsId, xmlBody);

        assertEquals(500, response.statusCode());
    }
}
