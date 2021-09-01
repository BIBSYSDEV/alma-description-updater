package no.unit.scheduler;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.InvalidMessageContentsException;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import no.unit.exceptions.SchedulerException;
import nva.commons.core.Environment;
import software.amazon.awssdk.regions.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SchedulerHelper {

    private static final String FILE_KEY = "files/";
    private static final String IMAGE_KEY = "images/";
    private static final String AUDIO_MP3_KEY = "audio/mp3";
    private static final String CONTENTS_URL_PART = "content/";
    private static final String CONTENT_URL_KEY = "STANDARD_CONTENT_URL";


    private static final String SMALL_KEY = "small";
    private static final String LARGE_KEY = "large";
    private static final String ORIGINAL_KEY = "original";
    private static final String SMALL_DESCRIPTION = "Miniatyrbilde";
    private static final String LARGE_DESCRIPTION = "Omslagsbilde";
    private static final String ORIGINAL_DESCRIPTION = "Originalt bilde";
    private static final String SHORT_KEY = "description_short";
    private static final String LONG_KEY = "description_long";
    private static final String CONTENTS_KEY = "contents";
    private static final String S = "S";
    private static final String MODIFY = "MODIFY";
    private static final String SHORT_DESCRIPTION = "Forlagets beskrivelse (kort)";
    private static final String LONG_DESCRIPTION = "Forlagets beskrivelse (lang)";
    private static final String CONTENTS_DESCRIPTION = "Innholdsfortegnelse";
    private static final String DLQ_QUEUE_URL_KEY = "DLQ_QUEUE_URL";

    private final transient Environment envHandler;


    public SchedulerHelper(Environment envHandler) {
        this.envHandler = envHandler;
    }

    public SchedulerHelper() {
        this.envHandler = new Environment();
    }


    /**
     * Creates a list of UpdateItem objects, if the event is "MODIFIED" this will be from the
     *     difference between the new and the old image, if created simply from the new image.
     * @param eventBody The body of the SQSEvent.
     * @return A list of UpdateItem objects.
     */
    public List<UpdateItem> splitEventIntoUpdateItems(String eventBody)  {
        JsonObject eventBodyObject = JsonParser.parseString(eventBody).getAsJsonObject();
        String isbn = eventBodyObject.get("dynamodb").getAsJsonObject().get("Keys")
                .getAsJsonObject().get("isbn").getAsJsonObject().get(S).getAsString();
        String eventName = eventBodyObject.get("eventName").getAsString();
        JsonObject newImage = eventBodyObject.get("dynamodb").getAsJsonObject().get("NewImage").getAsJsonObject();
        BibItem newBibItem = extractFromJsonObject(newImage);
        newBibItem.setIsbn(isbn);
        if (MODIFY.equals(eventName)) {
            JsonObject oldImage = eventBodyObject.get("dynamodb").getAsJsonObject().get("OldImage").getAsJsonObject();
            BibItem oldBibItem = extractFromJsonObject(oldImage);
            oldBibItem.setIsbn(isbn);

            BibItem diffBibItem = extractDiffs(newBibItem, oldBibItem);

            return createLinks(diffBibItem);
        } else {
            return createLinks(newBibItem);
        }
    }

    /**
     * Extracts the data needed to create an DynamoDbItem from a JsonObject.
     * @param image Either the new or the old image, containing the dynamoDbItem.
     * @return The DynamoDbItem.
     */
    private BibItem extractFromJsonObject(JsonObject image) {
        BibItem bibItem = new BibItem();
        if (image.get("description_short") != null) {
            bibItem.setDescriptionShort(image.get("description_short").getAsJsonObject().get(S).getAsString());
        }
        if (image.get("image_large") != null) {
            bibItem.setImageLarge(image.get("image_large").getAsJsonObject().get(S).getAsString());
        }
        if (image.get("description_long") != null) {
            bibItem.setDescriptionLong(image.get("description_long").getAsJsonObject().get(S).getAsString());
        }
        if (image.get("image_small") != null) {
            bibItem.setImageSmall(image.get("image_small").getAsJsonObject().get(S).getAsString());
        }
        if (image.get("table_of_contents") != null) {
            bibItem.setTableOfContents(image.get("table_of_contents").getAsJsonObject().get(S).getAsString());
        }
        if (image.get("image_original") != null) {
            bibItem.setImageOriginal(image.get("image_original").getAsJsonObject().get(S).getAsString());
        }
        if (image.get("audio_file") != null) {
            bibItem.setAudioFile(image.get("audio_file").getAsJsonObject().get(S).getAsString());
        }
        return bibItem;
    }

    /**
     * Creates a list of UpdateItem objects based on a list of DynamoDbItems.
     *     The DynamoDbItem may result in several UpdateItem objects.
     * @param item The DynamoDbItem from which to extract and create UpdateItems from.
     * @return A list of UpdateItems.
     */
    public List<UpdateItem> createLinks(BibItem item) {
        List<UpdateItem> items = new ArrayList<>();
        if (item.getDescriptionShort() != null) {
            items.add(createContentLink(SHORT_KEY, item.getIsbn()));
        }
        if (item.getDescriptionLong() != null) {
            items.add(createContentLink(LONG_KEY, item.getIsbn()));
        }
        if (item.getTableOfContents() != null) {
            items.add(createContentLink(CONTENTS_KEY, item.getIsbn()));
        }
        if (item.getImageSmall() != null) {
            items.add(createImageLink(SMALL_KEY, item.getIsbn()));
        }
        if (item.getImageLarge() != null) {
            items.add(createImageLink(LARGE_KEY, item.getIsbn()));
        }
        if (item.getImageOriginal() != null) {
            items.add(createImageLink(ORIGINAL_KEY, item.getIsbn()));
        }
        if (item.getAudioFile() != null) {
            items.add(createAudioLink(item.getIsbn()));
        }

        return items;
    }

    /**
     * Creates a UpdateItem containing the correct isbn, link and specifiedMaterial.
     * @param imageSize The size of the Image to create a link for.
     * @param isbn The isbn to create the UpdateItem for.
     * @return A UpdateItem.
     */
    public UpdateItem createImageLink(String imageSize, String isbn) {
        String secondLinkPart = isbn.substring(isbn.length() - 2, isbn.length() - 1);
        String firstLinkPart = isbn.substring(isbn.length() - 1);
        String link = String.format(envHandler.readEnv(CONTENT_URL_KEY) + FILE_KEY + IMAGE_KEY + imageSize
                + "/%s/%s/%s.jpg", firstLinkPart, secondLinkPart, isbn);

        String specifiedMaterial;
        switch (imageSize) {
            case SMALL_KEY:
                specifiedMaterial = SMALL_DESCRIPTION;
                break;
            case LARGE_KEY:
                specifiedMaterial = LARGE_DESCRIPTION;
                break;
            default:
                specifiedMaterial = ORIGINAL_DESCRIPTION;
                break;
        }
        UpdateItem item = new UpdateItem();
        item.setIsbn(isbn);
        item.setLink(link);
        item.setSpecifiedMaterial(specifiedMaterial);
        return item;
    }

    /**
     * Creates a UpdateItem containing the correct isbn, link and specifiedMaterial.
     * @param contentType The type of content to create a link for.
     * @param isbn The isbn to create the UpdateItem for.
     * @return A UpdateItem.
     */
    public UpdateItem createContentLink(String contentType, String isbn) {
        String link = envHandler.readEnv(CONTENT_URL_KEY) + CONTENTS_URL_PART + "?isbn=" + isbn;

        String specifiedMaterial;
        switch (contentType.toLowerCase(Locale.getDefault())) {
            case SHORT_KEY:
                specifiedMaterial = SHORT_DESCRIPTION;
                break;
            case LONG_KEY:
                specifiedMaterial = LONG_DESCRIPTION;
                break;
            default:
                specifiedMaterial = CONTENTS_DESCRIPTION;
                break;
        }
        UpdateItem item = new UpdateItem();
        item.setIsbn(isbn);
        item.setLink(link);
        item.setSpecifiedMaterial(specifiedMaterial);
        return item;
    }

    /**
     * Creates a UpdateItem containing the correct isbn, link and specifiedMaterial.
     * @param isbn The isbn to create the UpdateItem for.
     * @return A UpdateItem.
     */
    public UpdateItem createAudioLink(String isbn) {
        String secondLinkPart = isbn.substring(isbn.length() - 2, isbn.length() - 1);
        String firstLinkPart = isbn.substring(isbn.length() - 1);
        String link = String.format(envHandler.readEnv(CONTENT_URL_KEY) + FILE_KEY + AUDIO_MP3_KEY
                + "/%s/%s/%s.mp3", firstLinkPart, secondLinkPart, isbn);
        String specifiedMaterial = "Lydfil";
        UpdateItem item = new UpdateItem();
        item.setIsbn(isbn);
        item.setLink(link);
        item.setSpecifiedMaterial(specifiedMaterial);
        return item;
    }

    /**
     * Method to fill the actually updated fields of a BibItem.
     * @param newVersion BibItem containing the new version of the db-record.
     * @param oldVersion BibItem containing the old version of the db-record.
     * @return A BibItem with only the field of interest filed.
     */
    @SuppressWarnings("PMD.NPathComplexity")
    public BibItem extractDiffs(BibItem newVersion, BibItem oldVersion) {
        BibItem returnVersion = new BibItem();
        returnVersion.setIsbn(newVersion.getIsbn());
        if (newVersion.getDescriptionShort() != null && !newVersion.getDescriptionShort()
                .equals(oldVersion.getDescriptionShort())) {
            returnVersion.setDescriptionShort(newVersion.getDescriptionShort());
        }
        if (newVersion.getDescriptionLong() != null && !newVersion.getDescriptionLong()
                .equals(oldVersion.getDescriptionLong())) {
            returnVersion.setDescriptionLong(newVersion.getDescriptionLong());
        }
        if (newVersion.getTableOfContents() != null && !newVersion.getTableOfContents()
                .equals(oldVersion.getTableOfContents())) {
            returnVersion.setTableOfContents(newVersion.getTableOfContents());
        }
        if (newVersion.getImageSmall() != null && !newVersion.getImageSmall()
                .equals(oldVersion.getImageSmall())) {
            returnVersion.setImageSmall(newVersion.getImageSmall());
        }
        if (newVersion.getImageOriginal() != null && !newVersion.getImageOriginal()
                .equals(oldVersion.getImageOriginal())) {
            returnVersion.setImageOriginal(newVersion.getImageOriginal());
        }
        if (newVersion.getImageLarge() != null && !newVersion.getImageLarge()
                .equals(oldVersion.getImageLarge())) {
            returnVersion.setImageLarge(newVersion.getImageLarge());
        }
        if (newVersion.getAudioFile() != null && !newVersion.getAudioFile()
                .equals(oldVersion.getAudioFile())) {
            returnVersion.setAudioFile(newVersion.getAudioFile());
        }
        return returnVersion;
    }

    /**
     * Method that writes a message to an already specified queue.
     * @param message The message thats to be sent to the queue.
     * @throws SchedulerException when something goes wrong.
     */
    public void writeToDLQ(String message) throws SchedulerException {
        try {
            AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(Region.EU_WEST_1.toString()).build();
            String queueUrl = envHandler.readEnv(DLQ_QUEUE_URL_KEY);
            SendMessageRequest sendMsgRequest = new SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(message)
                    .withDelaySeconds(5);
            sqs.sendMessage(sendMsgRequest);
        } catch (InvalidMessageContentsException | UnsupportedOperationException e) {
            throw new SchedulerException("Failed to send message to DLQ. ", e);
        }
    }
}
