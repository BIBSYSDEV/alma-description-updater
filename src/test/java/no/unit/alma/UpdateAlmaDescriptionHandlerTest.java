package no.unit.alma;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Collections;
import no.unit.http.ReadConnection;
import no.unit.http.ReadConnectionFactory;
import no.unit.scheduler.SchedulerHelper;
import no.unit.scheduler.UpdateItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UpdateAlmaDescriptionHandlerTest {

    private static final String CORRECT_XML_FILE = "/Mock_xml.xml";
    private static final String UPDATED_XML_FILE = "/UpdatedGroupXml.xml";
    public static final String ALMA_SRU_PROXY_RESPONSE_JSON = "/alma_sru_proxy_response.json";
    public static final String ALMA_RESPONSE_MMS_ID_JSON = "/alma_response_mms_id.xml";
    private static final String XML_TITLE = "<title>Hobbiten : Smaugs ødemark i bilder</title>";

    @Mock
    private Context mockContext;

    @Mock
    private SchedulerHelper mockSchedulerHelper;

    @Mock
    private ReadConnection mockAlmaSruProxyConnection;

    @Mock
    private ReadConnectionFactory mockAlmaSruProxyFactory;

    @Mock
    private AlmaClient mockAlmaClient;

    private UpdateAlmaDescriptionHandler mockedHandler;

    private SQSEvent mockSqsEvent;

    /**
     * Sets up a mock environment for use when testing.
     */
    @BeforeEach
    @SuppressWarnings("resource")
    public void init() throws Exception {
        MockitoAnnotations.openMocks(this);

        mockSqsEvent = createDummySqsEvent();

        doNothing().when(mockSchedulerHelper).writeToDLQ(any());
        var updateItems = createUpdateItemList();
        doReturn(updateItems).when(mockSchedulerHelper).splitEventIntoUpdateItems(any());

        var almaSruProxyResponsePayload = setup(ALMA_SRU_PROXY_RESPONSE_JSON);
        var mockAlmaSruProxyHttpResponse = mock(HttpResponse.class);
        doReturn(almaSruProxyResponsePayload).when(mockAlmaSruProxyHttpResponse).body();
        doReturn(HTTP_OK).when(mockAlmaSruProxyHttpResponse).statusCode();
        doReturn(mockAlmaSruProxyHttpResponse).when(mockAlmaSruProxyConnection).sendGet(any());
        doReturn(mockAlmaSruProxyConnection).when(mockAlmaSruProxyFactory).create();

        var almaResponsePayload = setup(ALMA_RESPONSE_MMS_ID_JSON);
        var mockAlmaHttpResponse = mock(HttpResponse.class);
        doReturn(almaResponsePayload).when(mockAlmaHttpResponse).body();
        doReturn(HTTP_OK).when(mockAlmaHttpResponse).statusCode();
        doReturn(mockAlmaHttpResponse).when(mockAlmaClient).getBibRecordFromAlmaWithRetries(any());

        doReturn(mockAlmaHttpResponse).when(mockAlmaClient).putBibRecordInAlmaWithRetries(any(), any());

        mockedHandler = new UpdateAlmaDescriptionHandler(mockAlmaClient,
                                                         mockSchedulerHelper,
                                                         new DocumentXmlParser(),
                                                         new IsbnConverter(),
                                                         mockAlmaSruProxyFactory);
    }

    @Test
    public void shouldUpdateBibRecordAndSkipTheUpdatesThatAreEqual() throws Exception {
        var gson = new Gson();
        var mockXml = setup(CORRECT_XML_FILE);
        var item1String = "{isbn: 1234, link: 1234_small_1234.jpg, specifiedMaterial: Small_coverFoto}";
        var item2String = "{isbn: 1234, link: 1234_large_1234.jpg, specifiedMaterial: Large_coverFoto}";
        var item1 = gson.fromJson(item1String, UpdateItem.class);
        var item2 = gson.fromJson(item2String, UpdateItem.class);
        var updateItemList = new ArrayList<UpdateItem>();
        updateItemList.add(item1);
        updateItemList.add(item1);
        updateItemList.add(item2);
        var mockUpdatedXml = setup(UPDATED_XML_FILE);

        var updatedXml = mockedHandler.updateBibRecord(updateItemList, mockXml);

        assertEquals(mockUpdatedXml, updatedXml);
    }

    @Test
    public void shouldFetchFromAndUpdateAlmaWithUpdateItemsFromSqsEventWithoutError() throws Exception {
        final var getBibRecordCaptor = ArgumentCaptor.forClass(String.class);
        final var putBibRecordCaptor = ArgumentCaptor.forClass(String.class);
        final var getFromAlmaProxyCaptor = ArgumentCaptor.forClass(String.class);
        final var mmsId = "991325803064702201";

        final var response = mockedHandler.handleRequest(mockSqsEvent, mockContext);

        verify(mockAlmaSruProxyConnection, times(2))
            .sendGet(getFromAlmaProxyCaptor.capture());
        assertThat(getFromAlmaProxyCaptor.getAllValues(), containsInAnyOrder("9788210053412", "8210053418"));
        verify(mockAlmaClient, times(2))
            .getBibRecordFromAlmaWithRetries(getBibRecordCaptor.capture());
        assertThat(getBibRecordCaptor.getAllValues(), containsInAnyOrder(mmsId, mmsId));
        verify(mockAlmaClient, times(2))
            .putBibRecordInAlmaWithRetries(putBibRecordCaptor.capture(), any());
        assertThat(putBibRecordCaptor.getAllValues(), containsInAnyOrder(mmsId, mmsId));
        verify(mockSchedulerHelper, times(0)).writeToDLQ(any());
        assertThat(response, equalTo(null));
    }

    @Test
    public void shouldReturnNullAndWriteToDlqWhenFetchFromAlmaSruProxyFailsHavingTriedBothIsbn13AndIsbn10()
        throws Exception {

        var failedHttpResponse = mock(HttpResponse.class);
        doReturn(HTTP_UNAVAILABLE).when(failedHttpResponse).statusCode();
        var captor = ArgumentCaptor.forClass(String.class);
        doReturn(failedHttpResponse).when(mockAlmaSruProxyConnection).sendGet(captor.capture());

        var response = mockedHandler.handleRequest(mockSqsEvent, mockContext);

        assertThat(captor.getAllValues(), containsInAnyOrder("9788210053412", "8210053418"));
        verify(mockSchedulerHelper, times(1)).writeToDLQ(any());
        assertThat(response, equalTo(null));
    }

    @Test
    public void shouldSkipUsingConvertedIsbnWhenFetchConvertedIsbnFromSruProxyFails()
        throws Exception {

        var failedHttpResponse = mock(HttpResponse.class);
        doReturn(HTTP_UNAVAILABLE).when(failedHttpResponse).statusCode();
        doReturn(failedHttpResponse).when(mockAlmaSruProxyConnection).sendGet("8210053418");

        final var response = mockedHandler.handleRequest(mockSqsEvent, mockContext);

        verify(mockSchedulerHelper, times(0)).writeToDLQ(any());
        verify(mockAlmaClient, times(1)).getBibRecordFromAlmaWithRetries(any());
        verify(mockAlmaClient, times(1)).putBibRecordInAlmaWithRetries(any(), any());
        assertThat(response, equalTo(null));
    }

    @Test
    public void shouldHandleErrorWhenCreatingUpdateItemsFails() {
        doThrow(RuntimeException.class).when(mockSchedulerHelper).splitEventIntoUpdateItems(any());

        var exception = assertThrows(RuntimeException.class, () -> mockedHandler.handleRequest(mockSqsEvent,
                                                                                               mockContext));

        assertThat(exception.getMessage(), containsString("Error while processing input event."));
    }

    @Test
    public void shouldNotUpdateWhenUpdateItemContainsDataWeDoNotWantToUpdate() throws Exception {
        doReturn(Collections.emptyList()).when(mockSchedulerHelper).splitEventIntoUpdateItems(any());

        final var response = mockedHandler.handleRequest(mockSqsEvent, mockContext);

        verify(mockAlmaSruProxyConnection, times(0)).sendGet(any());
        verify(mockAlmaClient, times(0)).getBibRecordFromAlmaWithRetries(any());
        verify(mockAlmaClient, times(0)).putBibRecordInAlmaWithRetries(any(), any());
        assertThat(response, equalTo(null));
    }

    @Test
    public void shouldHandleErrorsThatOccursWhenUpdatingFailsBecauseOfSomeGeneralException() throws Exception {
        doThrow(IOException.class).when(mockAlmaSruProxyConnection).sendGet(any());

        var exception = assertThrows(RuntimeException.class,
                                     () -> mockedHandler.handleRequest(mockSqsEvent, mockContext));

        assertThat(exception.getMessage(), containsString("General error:"));
    }

    @Test
    public void shouldThrowExceptionWhenAllFetchFromAlmaReturnsStatusOtherThanOk() throws Exception {
        var httpResponse = mock(HttpResponse.class);
        doReturn(HTTP_UNAVAILABLE).when(httpResponse).statusCode();
        doReturn(httpResponse)
            .doReturn(httpResponse)
            .when(mockAlmaClient).getBibRecordFromAlmaWithRetries(any());

        var response = assertThrows(RuntimeException.class,
                                    () -> mockedHandler.handleRequest(mockSqsEvent, mockContext));

        assertThat(response.getMessage(), containsString("1 or more mms_id's did not go through with mms_id: "));
        assertThat(response.getMessage(), containsString("Get failed"));
    }

    @Test
    public void shouldThrowExceptionWhenAllUpdateInAlmaReturnsStatusOtherThanOk() throws Exception {
        var httpResponse = mock(HttpResponse.class);
        doReturn(HTTP_UNAVAILABLE).when(httpResponse).statusCode();
        doReturn(httpResponse)
            .doReturn(httpResponse)
            .when(mockAlmaClient).putBibRecordInAlmaWithRetries(any(), any());

        var response = assertThrows(RuntimeException.class,
                                    () -> mockedHandler.handleRequest(mockSqsEvent, mockContext));

        assertThat(response.getMessage(), containsString("1 or more mms_id's did not go through with mms_id: "));
        assertThat(response.getMessage(), containsString("Get response"));
        assertThat(response.getMessage(), containsString(XML_TITLE));
        assertThat(response.getMessage(), not(containsString("Put response")));
    }

    @Test
    public void shouldThrowExceptionWhenNotAllFetchAndUpdateSucceeds() throws Exception {
        var httpResponseUnavailable = mock(HttpResponse.class);

        var almaResponsePayload = setup(ALMA_RESPONSE_MMS_ID_JSON);
        var almaOkHttpResponse = mock(HttpResponse.class);
        doReturn(almaResponsePayload).when(almaOkHttpResponse).body();
        doReturn(HTTP_OK).when(almaOkHttpResponse).statusCode();

        doReturn(almaOkHttpResponse).when(mockAlmaClient).putBibRecordInAlmaWithRetries(any(), any());

        doReturn(HTTP_UNAVAILABLE).when(httpResponseUnavailable).statusCode();
        doReturn(httpResponseUnavailable)
            .doReturn(almaOkHttpResponse)
            .when(mockAlmaClient).getBibRecordFromAlmaWithRetries(any());

        var response = assertThrows(RuntimeException.class,
                                    () -> mockedHandler.handleRequest(mockSqsEvent, mockContext));

        assertThat(response.getMessage(), containsString("1 or more mms_id's did not go through with mms_id: "));
        assertThat(response.getMessage(), containsString("Get response"));
        assertThat(response.getMessage(), containsString("Put response"));
        assertThat(response.getMessage(), containsString(XML_TITLE));
    }

    private SQSEvent createDummySqsEvent() {
        var sqsEvent = new SQSEvent();
        var sqsMessage = new SQSMessage();
        sqsMessage.setMessageId("sqsMessageId");
        sqsMessage.setBody("sqsBody");
        sqsEvent.setRecords(List.of(sqsMessage));

        return sqsEvent;
    }

    /**
     * A helper method that returns a string from a source.
     * @param file The file/source you want to retrieve the string from.
     * @return A string-value representing the content of the source.
     * @throws Exception when something goes wrong.
     */
    private String setup(String file) throws Exception {
        var stream = DocumentXmlParserTest.class.getResourceAsStream(file);
        if (stream == null) {
            throw new RuntimeException("Cannot find resource " + file);
        }
        var reader = new InputStreamReader(stream);
        var br = new BufferedReader(reader);
        String line;
        var sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        return sb.toString();
    }

    private List<UpdateItem> createUpdateItemList() {
        var updateItems = new ArrayList<UpdateItem>();

        updateItems.add(
            new UpdateItem("9788210053412",
                           "content-url-com/content/?isbn=9788210053412",
                           "Forlagets beskrivelse (kort)")
        );
        updateItems.add(
            new UpdateItem("9788210053412",
                           "content-url-com/content/?isbn=9788210053412",
                           "Forlagets beskrivelse (lang)")
        );
        updateItems.add(
            new UpdateItem("9788210053412",
                           "content-url-com/content/?isbn=9788210053412",
                           "Innholdsfortegnelse")
        );
        updateItems.add(
            new UpdateItem("9788210053412",
                           "content-url-com/files/images/small/2/1/9788210053412.jpg",
                           "Miniatyrbilde")
        );
        updateItems.add(
            new UpdateItem("9788210053412",
                           "content-url-com/files/images/large/2/1/9788210053412.jpg",
                           "Omslagsbilde")
        );
        updateItems.add(
            new UpdateItem("9788210053412",
                            "content-url-com/files/images/original/2/1/9788210053412.jpg",
                            "Originalt bilde")
        );
        updateItems.add(
            new UpdateItem("9788210053412",
                           "content-url-com/files/audio/mp3/2/1/9788210053412.mp3",
                           "Lydfil")
        );

        return updateItems;
    }

}
