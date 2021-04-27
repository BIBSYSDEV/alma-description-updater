package no.unit.scheduler;


import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.alma.XmlParserTest;
import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchedulerHelperTest {

    private static final String IMAGE_KEY = "image/";
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

    public void printList(List<?> theList) {
        Iterator<?> iter = theList.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            System.out.println(item.toString());
        }
    }

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
    public void init() {
        mockEnv = mock(Environment.class);
        initEnv();
        mockSchedulerHelper = new SchedulerHelper(mockEnv);
    }

    @Test
    void generateImageLinkTest() throws Exception {
        UpdateItem payload = mockSchedulerHelper.createImageLink(IMAGE_SIZE, ISBN);
        String expectedLink = String.format(CONTENT_URL_KEY + IMAGE_KEY + IMAGE_SIZE + "/%s/%s/%s.jpg", 7, 4, ISBN);
        assertEquals(expectedLink, payload.getLink());
    }

    @Test
    void generateContentLinkTest() throws Exception {
        UpdateItem payload = mockSchedulerHelper.createContentLink(CONTENT_TYPE, ISBN);
        String expectedLink = String.format(CONTENT_URL_KEY + ISBN + "?type=" + CONTENT_TYPE.toUpperCase());
        assertEquals(expectedLink, payload.getLink());
    }

    @Test
    void generateAudioLinkTest() throws Exception {
        UpdateItem payload = mockSchedulerHelper.createAudioLink(ISBN);
        String expectedLink = String.format(CONTENT_URL_KEY + AUDIO_MP3_KEY + "/%s/%s/%s.mp3", 7, 4, ISBN);
        assertEquals(expectedLink, payload.getLink());
    }

    @Test
    void extractDiffsTest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
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
}