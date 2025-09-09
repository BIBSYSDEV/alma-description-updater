package no.unit.alma;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.net.http.HttpResponse;
import no.unit.http.Connection;
import no.unit.http.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AlmaClientTest {

    public static final String PAYLOAD = "<body>Hello</body>";
    public static final String MMS_ID = "1234";

    @Mock
    private Connection mockConnection;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @Mock
    private ConnectionFactory mockConnectionFactory;

    @InjectMocks
    private AlmaClient almaClient;

    @BeforeEach
    @SuppressWarnings("resource")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        doReturn(mockConnection).when(mockConnectionFactory).create();

        almaClient = new AlmaClient(mockConnectionFactory);
    }

    @Test
    void shouldGetBibRecordFromAlma() throws Exception {
        doReturn(HTTP_OK).when(mockHttpResponse).statusCode();
        doReturn(PAYLOAD).when(mockHttpResponse).body();
        doReturn(mockHttpResponse).when(mockConnection).sendGet(any());

        var response = almaClient.getBibRecordFromAlmaWithRetries(MMS_ID);

        verify(mockConnection, times(1)).sendGet(MMS_ID);
        assertNotNull(response);
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.body(), equalTo(PAYLOAD));
    }

    @Test
    void shouldGetBibRecordFromAlmaWithRetryOnError() throws Exception {
        doReturn(HTTP_OK).when(mockHttpResponse).statusCode();
        doReturn(PAYLOAD).when(mockHttpResponse).body();
        doThrow(IOException.class)
            .doReturn(mockHttpResponse)
            .when(mockConnection).sendGet(any());

        var response = almaClient.getBibRecordFromAlmaWithRetries(MMS_ID);

        verify(mockConnection, times(2)).sendGet(MMS_ID);
        assertNotNull(response);
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.body(), equalTo(PAYLOAD));
    }

    @Test
    void shouldGetBibRecordFromAlmaAfterSecondRetryOnError() throws Exception {
        doReturn(HTTP_OK).when(mockHttpResponse).statusCode();
        doReturn(PAYLOAD).when(mockHttpResponse).body();
        doThrow(IOException.class)
            .doThrow(InterruptedException.class)
            .doReturn(mockHttpResponse)
            .when(mockConnection).sendGet(any());

        var response = almaClient.getBibRecordFromAlmaWithRetries(MMS_ID);

        verify(mockConnection, times(3)).sendGet(MMS_ID);
        assertNotNull(response);
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.body(), equalTo(PAYLOAD));
    }

    @Test
    void shouldGetBibRecordFromAlmaEvenWhenFirstAttemptGivesStatusOtherThan200Ok() throws Exception {
        doReturn(HTTP_UNAVAILABLE).when(mockHttpResponse).statusCode();

        var mockSecondResponse = mock(HttpResponse.class);
        doReturn(HTTP_OK).when(mockSecondResponse).statusCode();
        doReturn(PAYLOAD).when(mockSecondResponse).body();

        doReturn(mockHttpResponse)
            .doReturn(mockSecondResponse)
            .when(mockConnection).sendGet(any());

        var response = almaClient.getBibRecordFromAlmaWithRetries(MMS_ID);

        verify(mockConnection, times(2)).sendGet(MMS_ID);
        assertNotNull(response);
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.body(), equalTo(PAYLOAD));
    }

    @Test
    void shouldGetBibRecordFromAlmaEvenWhenFirstAndSecondAttemptGivesStatusOtherThan200Ok() throws Exception {
        doReturn(HTTP_UNAVAILABLE).when(mockHttpResponse).statusCode();

        var mockSecondResponse = mock(HttpResponse.class);
        doReturn(HTTP_UNAVAILABLE).when(mockSecondResponse).statusCode();

        var mockThirdResponse = mock(HttpResponse.class);
        doReturn(HTTP_OK).when(mockThirdResponse).statusCode();
        doReturn(PAYLOAD).when(mockThirdResponse).body();

        doReturn(mockHttpResponse)
            .doReturn(mockSecondResponse)
            .doReturn(mockThirdResponse)
            .when(mockConnection).sendGet(any());

        var response = almaClient.getBibRecordFromAlmaWithRetries(MMS_ID);

        verify(mockConnection, times(3)).sendGet(MMS_ID);
        assertNotNull(response);
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.body(), equalTo(PAYLOAD));
    }

}