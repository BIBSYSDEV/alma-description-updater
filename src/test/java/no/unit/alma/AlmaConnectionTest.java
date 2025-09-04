package no.unit.alma;

import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static no.unit.alma.Config.ALMA_API_HOST_KEY;
import static no.unit.alma.Config.ALMA_API_KEY;
import static no.unit.alma.Config.ALMA_SRU_HOST_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlmaConnectionTest {

    private AlmaConnection almaConnection;
    private HttpClient httpClientMock;
    private HttpResponse<String> httpResponseMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClientMock = mock(HttpClient.class);
        httpResponseMock = mock(HttpResponse.class);
        var envMock = mock(Environment.class);

        doReturn("http://mock-alma-api-host/").when(envMock).readEnv(ALMA_API_HOST_KEY);
        doReturn("http://mock-alma-sru-host/").when(envMock).readEnv(ALMA_SRU_HOST_KEY);
        doReturn("mock-api-key").when(envMock).readEnv(ALMA_API_KEY);
        var config = new Config(envMock);

        almaConnection = new AlmaConnection(config, httpClientMock);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnStatusOkWithResponseBodyOnSuccessfulGetRequest() throws IOException, InterruptedException {
        var mmsId = "123456";
        var expectedResponseBody = "<response>Success</response>";

        when(httpResponseMock.body()).thenReturn(expectedResponseBody);
        when(httpResponseMock.statusCode()).thenReturn(200);
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponseMock);

        var response = almaConnection.sendGet(mmsId);

        assertEquals(200, response.statusCode());
        assertEquals(expectedResponseBody, response.body());

        var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClientMock).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

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

        when(httpResponseMock.body()).thenReturn(expectedResponseBody);
        when(httpResponseMock.statusCode()).thenReturn(200);
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponseMock);

        var response = almaConnection.sendPut(mmsId, xmlBody);

        assertEquals(200, response.statusCode());
        assertEquals(expectedResponseBody, response.body());

        var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClientMock).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

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
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("Mock IOException"));

        var exception = assertThrows(IOException.class, () -> almaConnection.sendGet(mmsId));
        assertEquals("Mock IOException", exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnInternalServerErrorStatusCodeWhenSendPutFail() throws IOException, InterruptedException {
        var mmsId = "123456";
        var xmlBody = "<xml>Updated Content</xml>";

        when(httpResponseMock.statusCode()).thenReturn(500);
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponseMock);

        var response = almaConnection.sendPut(mmsId, xmlBody);

        assertEquals(500, response.statusCode());
    }
}