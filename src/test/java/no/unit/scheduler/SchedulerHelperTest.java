package no.unit.scheduler;


import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.alma.XmlParserTest;
import no.unit.aws.SqsClientFactory;
import no.unit.exceptions.SchedulerException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static no.unit.scheduler.SchedulerHelper.DLQ_QUEUE_URL_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

    Environment mockEnv;
    SchedulerHelper mockSchedulerHelper;
    ObjectMapper objectMapper = new ObjectMapper();
    SqsClient mockSqsClient;

    public String setup(String file) throws Exception {
        InputStream stream = XmlParserTest.class.getResourceAsStream(file);
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
        mockEnv = mock(Environment.class);
        var mockSqsClientFactory = mock(SqsClientFactory.class);
        mockSqsClient = mock(SqsClient.class);
        doReturn(mockSqsClient).when(mockSqsClientFactory).createSqsClient();
        initEnv();
        mockSchedulerHelper = new SchedulerHelper(mockEnv, mockSqsClientFactory);
    }

    @Test
    void generateImageLinkTest() {
        UpdateItem payload = mockSchedulerHelper.createImageLink(IMAGE_SIZE, ISBN);
        String expectedLink = String.format(CONTENT_URL_KEY + FILE_KEY + IMAGE_KEY + IMAGE_SIZE
                + "/%s/%s/%s.jpg", 7, 4, ISBN);
        assertEquals(expectedLink, payload.getLink());
    }

    @Test
    void generateContentLinkTest() {
        UpdateItem payload = mockSchedulerHelper.createContentLink(CONTENT_TYPE, ISBN);
        String expectedLink = String.format(CONTENT_URL_KEY  + "content/" + "?isbn=" + ISBN);
        assertEquals(expectedLink, payload.getLink());
    }

    @Test
    void generateAudioLinkTest() {
        UpdateItem payload = mockSchedulerHelper.createAudioLink(ISBN);
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
        BibItem theItem = mockSchedulerHelper.extractDiffs(newItem, oldItem);
        assertEquals(returnItem.toString(), theItem.toString());
    }

    @Test
    public void mockingEventTest() throws Exception {
        String mockEvent = setup(MOCKEVENT_FILE);
        List<UpdateItem> payloadList = mockSchedulerHelper.splitEventIntoUpdateItems(mockEvent);
        for (UpdateItem payload: payloadList) {
            System.out.println(payload.toString());
        }
    }

    @Test
    public void shouldWriteToDlqWithCorrectContent() throws Exception {
        doReturn("someDlqUrl").when(mockEnv).readEnv(DLQ_QUEUE_URL_KEY);

        mockSchedulerHelper.writeToDLQ("message");

        var captor = ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(mockSqsClient, times(1)).sendMessage(captor.capture());
        assertEquals("message", captor.getValue().messageBody());
        assertEquals(5, captor.getValue().delaySeconds());
        assertEquals("someDlqUrl", captor.getValue().queueUrl());
    }

    @Test
    public void shouldThrowExceptionWhenWriteToDlqFails() {
        doThrow(UnsupportedOperationException.class).when(mockSqsClient).sendMessage(any(SendMessageRequest.class));

        assertThrows(SchedulerException.class, () -> mockSchedulerHelper.writeToDLQ("message"));
    }

}