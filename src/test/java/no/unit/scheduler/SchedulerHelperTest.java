package no.unit.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import no.unit.alma.DocumentXmlParserTest;
import no.unit.aws.SqsClientFactory;
import no.unit.exceptions.SchedulerException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static java.util.Objects.isNull;
import static no.unit.scheduler.SchedulerHelper.DLQ_QUEUE_URL_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchedulerHelperTest {

    private static final String FILE_KEY = "files/";
    private static final String IMAGE_KEY = "images/";
    private static final String AUDIO_MP3_KEY = "audio/mp3";
    private static final String CONTENT_URL_KEY = "content-url-com/";
    private static final String ISBN = "9788205377547";
    private static final String IMAGE_SIZE = "small";
    private static final String CONTENT_TYPE = "description_short";
    private static final String NEWVERSION = "/newVersion.JSON";
    private static final String OLDVERSION = "/oldVersion.JSON";
    private static final String RETURNVERSION = "/returnVersion.JSON";
    public static final String MOCKEVENT_FILE = "/MockEvent.JSON";
    public static final String MOCKEVENT_FILE_MISSING_FIELDS = "/mock_event_missing_fields.json";
    public static final String MOCKEVENT_FILE_EQUAL_FIELDS = "/mock_event_equal_fields.json";

    @Mock
    private Environment mockEnv;

    @Mock
    private SqsClient mockSqsClient;

    @Mock
    private SqsClientFactory mockSqsClientFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SchedulerHelper schedulerHelper;

    public String setup(String file) throws Exception {
        InputStream stream = DocumentXmlParserTest.class.getResourceAsStream(file);
        if (isNull(stream)) {
            throw new RuntimeException("Could not load xml file " + file);
        }
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(reader);
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        return sb.toString();
    }

    private void initEnv() {
        when(mockEnv.readEnv("STANDARD_CONTENT_URL")).thenReturn(CONTENT_URL_KEY);
    }

    /**
     * Sets up a mock environment for use when testing.
     */
    @BeforeEach
    @SuppressWarnings("resource")
    public void init() {
        MockitoAnnotations.openMocks(this);
        doReturn(mockSqsClient).when(mockSqsClientFactory).create();
        initEnv();
        schedulerHelper = new SchedulerHelper(mockEnv, mockSqsClientFactory);
    }

    @Test
    void generateImageLinkTest() {
        UpdateItem payload = schedulerHelper.createImageLink(IMAGE_SIZE, ISBN);
        String expectedLink = String.format(CONTENT_URL_KEY + FILE_KEY + IMAGE_KEY + IMAGE_SIZE
                                            + "/%s/%s/%s.jpg", 7, 4, ISBN);
        assertEquals(expectedLink, payload.getLink());
    }

    @Test
    void generateContentLinkTest() {
        UpdateItem payload = schedulerHelper.createContentLink(CONTENT_TYPE, ISBN);
        String expectedLink = String.format(CONTENT_URL_KEY + "content/" + "?isbn=" + ISBN);
        assertEquals(expectedLink, payload.getLink());
    }

    @Test
    void generateAudioLinkTest() {
        UpdateItem payload = schedulerHelper.createAudioLink(ISBN);
        String expectedLink = String.format(CONTENT_URL_KEY + FILE_KEY + AUDIO_MP3_KEY + "/%s/%s/%s.mp3", 7, 4, ISBN);
        assertEquals(expectedLink, payload.getLink());
    }

    @Test
    void extractDiffsTest() throws Exception {
        String oldVersion = setup(OLDVERSION);
        String newVersion = setup(NEWVERSION);
        String returnVersion = setup(RETURNVERSION);
        BibItem oldItem = objectMapper.readValue(oldVersion, BibItem.class);
        BibItem newItem = objectMapper.readValue(newVersion, BibItem.class);
        BibItem returnItem = objectMapper.readValue(returnVersion, BibItem.class);
        BibItem theItem = schedulerHelper.extractDiffs(newItem, oldItem);
        assertEquals(returnItem.toString(), theItem.toString());
    }

    @Test
    public void shouldCreateUpdateItemFromModifyEvent() throws Exception {
        var mockEvent = setup(MOCKEVENT_FILE);
        var payloadList = schedulerHelper.splitEventIntoUpdateItems(mockEvent);

        var expectedList = List.of(
            new UpdateItem("9788210053412",
                           "content-url-com/content/?isbn=9788210053412",
                           "Forlagets beskrivelse (kort)"),
            new UpdateItem("9788210053412",
                           "content-url-com/content/?isbn=9788210053412",
                           "Forlagets beskrivelse (lang)"),
            new UpdateItem("9788210053412",
                           "content-url-com/content/?isbn=9788210053412",
                           "Innholdsfortegnelse"),
            new UpdateItem("9788210053412",
                           "content-url-com/files/images/small/2/1/9788210053412.jpg",
                           "Miniatyrbilde"),
            new UpdateItem("9788210053412",
                           "content-url-com/files/images/large/2/1/9788210053412.jpg",
                           "Omslagsbilde"),
            new UpdateItem("9788210053412",
                           "content-url-com/files/images/original/2/1/9788210053412.jpg",
                           "Originalt bilde"),
            new UpdateItem("9788210053412",
                           "content-url-com/files/audio/mp3/2/1/9788210053412.mp3",
                           "Lydfil")
        );

        assertThat(payloadList, containsInAnyOrder(expectedList.toArray()));
    }

    @Test
    public void shouldCreateUpdateItemFromNewEvent() throws Exception {
        var mockEvent = setup(MOCKEVENT_FILE);
        mockEvent = mockEvent.replace("MODIFY", "NEW");
        var payloadList = schedulerHelper.splitEventIntoUpdateItems(mockEvent);

        var expectedList = List.of(
            new UpdateItem("9788210053412",
                           "content-url-com/content/?isbn=9788210053412",
                           "Forlagets beskrivelse (kort)"),
            new UpdateItem("9788210053412",
                           "content-url-com/content/?isbn=9788210053412",
                           "Forlagets beskrivelse (lang)"),
            new UpdateItem("9788210053412",
                           "content-url-com/content/?isbn=9788210053412",
                           "Innholdsfortegnelse"),
            new UpdateItem("9788210053412",
                           "content-url-com/files/images/small/2/1/9788210053412.jpg",
                           "Miniatyrbilde"),
            new UpdateItem("9788210053412",
                           "content-url-com/files/images/large/2/1/9788210053412.jpg",
                           "Omslagsbilde"),
            new UpdateItem("9788210053412",
                           "content-url-com/files/images/original/2/1/9788210053412.jpg",
                           "Originalt bilde"),
            new UpdateItem("9788210053412",
                           "content-url-com/files/audio/mp3/2/1/9788210053412.mp3",
                           "Lydfil")
        );

        assertThat(payloadList, containsInAnyOrder(expectedList.toArray()));
    }

    @Test
    public void shouldIgnoreMissingFieldsFromEvent() throws Exception {
        var mockEvent = setup(MOCKEVENT_FILE_MISSING_FIELDS);
        var payloadList = schedulerHelper.splitEventIntoUpdateItems(mockEvent);

        assertThat(payloadList, is(empty()));
    }

    @Test
    public void shouldIgnoreEqualFieldsFromEvent() throws Exception {
        var mockEvent = setup(MOCKEVENT_FILE_EQUAL_FIELDS);
        var payloadList = schedulerHelper.splitEventIntoUpdateItems(mockEvent);

        assertThat(payloadList, is(empty()));
    }

    @Test
    public void shouldWriteToDlqWithCorrectContent() throws Exception {
        doReturn("someDlqUrl").when(mockEnv).readEnv(DLQ_QUEUE_URL_KEY);

        schedulerHelper.writeToDLQ("message");

        var captor = ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(mockSqsClient, times(1)).sendMessage(captor.capture());
        assertEquals("message", captor.getValue().messageBody());
        assertEquals(5, captor.getValue().delaySeconds());
        assertEquals("someDlqUrl", captor.getValue().queueUrl());
    }

    @Test
    public void shouldThrowExceptionWhenWriteToDlqFails() {
        doThrow(UnsupportedOperationException.class).when(mockSqsClient).sendMessage(any(SendMessageRequest.class));

        assertThrows(SchedulerException.class, () -> schedulerHelper.writeToDLQ("message"));
    }
}
