package no.unit.scheduler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.function.Consumer;
import java.util.function.Supplier;
import no.unit.aws.DefaultSqsClientFactory;
import no.unit.aws.SqsClientFactory;
import no.unit.exceptions.SchedulerException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

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
    public static final String DLQ_QUEUE_URL_KEY = "DLQ_QUEUE_URL";
    private static final String IMAGE_LARGE = "image_large";
    private static final String IMAGE_SMALL = "image_small";
    private static final String TABLE_OF_CONTENTS = "table_of_contents";
    private static final String IMAGE_ORIGINAL = "image_original";
    private static final String AUDIO_FILE = "audio_file";

    private final transient Environment envHandler;
    private final transient SqsClient sqsClient;

    @JacocoGenerated
    public SchedulerHelper() {
        this(new Environment(), new DefaultSqsClientFactory());
    }

    public SchedulerHelper(Environment envHandler, SqsClientFactory sqsClientFactory) {
        this.envHandler = envHandler;
        this.sqsClient = sqsClientFactory.create();
    }


    /**
     * Creates a list of UpdateItem objects, if the event is "MODIFIED" this will be from the
     * difference between the new and the old image, if created simply from the new image.
     *
     * @param eventBody The body of the SQSEvent.
     * @return A list of UpdateItem objects.
     */
    public List<UpdateItem> splitEventIntoUpdateItems(String eventBody) {
        var eventBodyObject = JsonParser.parseString(eventBody).getAsJsonObject();
        var isbn = eventBodyObject.get("dynamodb").getAsJsonObject().get("Keys")
                       .getAsJsonObject().get("isbn").getAsJsonObject().get(S).getAsString();
        var eventName = eventBodyObject.get("eventName").getAsString();
        var newImage = eventBodyObject.get("dynamodb").getAsJsonObject().get("NewImage").getAsJsonObject();
        var newBibItem = extractFromJsonObject(newImage);
        newBibItem.setIsbn(isbn);
        if (MODIFY.equals(eventName)) {
            var oldImage = eventBodyObject.get("dynamodb").getAsJsonObject().get("OldImage").getAsJsonObject();
            var oldBibItem = extractFromJsonObject(oldImage);
            oldBibItem.setIsbn(isbn);

            var diffBibItem = extractDiffs(newBibItem, oldBibItem);

            return createLinks(diffBibItem);
        } else {
            return createLinks(newBibItem);
        }
    }

    /**
     * Extracts the data needed to create an DynamoDbItem from a JsonObject.
     *
     * @param image Either the new or the old image, containing the dynamoDbItem.
     * @return The DynamoDbItem.
     */
    private BibItem extractFromJsonObject(JsonObject image) {
        var bibItem = new BibItem();

        setIfPresent(image, SHORT_KEY, bibItem::setDescriptionShort);
        setIfPresent(image, IMAGE_LARGE, bibItem::setImageLarge);
        setIfPresent(image, LONG_KEY, bibItem::setDescriptionLong);
        setIfPresent(image, IMAGE_SMALL, bibItem::setImageSmall);
        setIfPresent(image, TABLE_OF_CONTENTS, bibItem::setTableOfContents);
        setIfPresent(image, IMAGE_ORIGINAL, bibItem::setImageOriginal);
        setIfPresent(image, AUDIO_FILE, bibItem::setAudioFile);

        return bibItem;
    }

    private void setIfPresent(JsonObject json, String key, Consumer<String> setter) {
        if (json.get(key) != null) {
            var value = json.get(key).getAsJsonObject().get(S).getAsString();
            setter.accept(value);
        }
    }

    /**
     * Creates a list of UpdateItem objects based on a list of DynamoDbItems.
     * The DynamoDbItem may result in several UpdateItem objects.
     *
     * @param item The DynamoDbItem from which to extract and create UpdateItems from.
     * @return A list of UpdateItems.
     */
    protected List<UpdateItem> createLinks(BibItem item) {
        List<UpdateItem> items = new ArrayList<>();

        addIfNotNull(item.getDescriptionShort(), () -> createContentLink(SHORT_KEY, item.getIsbn()), items);
        addIfNotNull(item.getDescriptionLong(), () -> createContentLink(LONG_KEY, item.getIsbn()), items);
        addIfNotNull(item.getTableOfContents(), () -> createContentLink(CONTENTS_KEY, item.getIsbn()), items);
        addIfNotNull(item.getImageSmall(), () -> createImageLink(SMALL_KEY, item.getIsbn()), items);
        addIfNotNull(item.getImageLarge(), () -> createImageLink(LARGE_KEY, item.getIsbn()), items);
        addIfNotNull(item.getImageOriginal(), () -> createImageLink(ORIGINAL_KEY, item.getIsbn()), items);
        addIfNotNull(item.getAudioFile(), () -> createAudioLink(item.getIsbn()), items);

        return items;
    }

    private <T> void addIfNotNull(T value, Supplier<UpdateItem> supplier, List<UpdateItem> items) {
        if (value != null) {
            items.add(supplier.get());
        }
    }

    /**
     * Creates a UpdateItem containing the correct isbn, link and specifiedMaterial.
     *
     * @param imageSize The size of the Image to create a link for.
     * @param isbn      The isbn to create the UpdateItem for.
     * @return A UpdateItem.
     */
    protected UpdateItem createImageLink(String imageSize, String isbn) {
        var secondLinkPart = isbn.substring(isbn.length() - 2, isbn.length() - 1);
        var firstLinkPart = isbn.substring(isbn.length() - 1);
        var link = String.format(envHandler.readEnv(CONTENT_URL_KEY) + FILE_KEY + IMAGE_KEY + imageSize
                                 + "/%s/%s/%s.jpg", firstLinkPart, secondLinkPart, isbn);

        var specifiedMaterial = switch (imageSize) {
            case SMALL_KEY -> SMALL_DESCRIPTION;
            case LARGE_KEY -> LARGE_DESCRIPTION;
            default -> ORIGINAL_DESCRIPTION;
        };

        var item = new UpdateItem();
        item.setIsbn(isbn);
        item.setLink(link);
        item.setSpecifiedMaterial(specifiedMaterial);

        return item;
    }

    /**
     * Creates a UpdateItem containing the correct isbn, link and specifiedMaterial.
     *
     * @param contentType The type of content to create a link for.
     * @param isbn        The isbn to create the UpdateItem for.
     * @return A UpdateItem.
     */
    protected UpdateItem createContentLink(String contentType, String isbn) {
        var link = envHandler.readEnv(CONTENT_URL_KEY) + CONTENTS_URL_PART + "?isbn=" + isbn;

        var specifiedMaterial = switch (contentType.toLowerCase(Locale.getDefault())) {
            case SHORT_KEY -> SHORT_DESCRIPTION;
            case LONG_KEY -> LONG_DESCRIPTION;
            default -> CONTENTS_DESCRIPTION;
        };
        var item = new UpdateItem();
        item.setIsbn(isbn);
        item.setLink(link);
        item.setSpecifiedMaterial(specifiedMaterial);

        return item;
    }

    /**
     * Creates a UpdateItem containing the correct isbn, link and specifiedMaterial.
     *
     * @param isbn The isbn to create the UpdateItem for.
     * @return A UpdateItem.
     */
    protected UpdateItem createAudioLink(String isbn) {
        var secondLinkPart = isbn.substring(isbn.length() - 2, isbn.length() - 1);
        var firstLinkPart = isbn.substring(isbn.length() - 1);
        var link = String.format(envHandler.readEnv(CONTENT_URL_KEY) + FILE_KEY + AUDIO_MP3_KEY
                                 + "/%s/%s/%s.mp3", firstLinkPart, secondLinkPart, isbn);
        var specifiedMaterial = "Lydfil";
        var item = new UpdateItem();
        item.setIsbn(isbn);
        item.setLink(link);
        item.setSpecifiedMaterial(specifiedMaterial);

        return item;
    }

    /**
     * Method to fill the actually updated fields of a BibItem.
     *
     * @param newVersion BibItem containing the new version of the db-record.
     * @param oldVersion BibItem containing the old version of the db-record.
     * @return A BibItem with only the field of interest filed.
     */
    protected BibItem extractDiffs(BibItem newVersion, BibItem oldVersion) {
        var returnVersion = new BibItem();
        returnVersion.setIsbn(newVersion.getIsbn());

        copyIfChanged(newVersion.getDescriptionShort(), oldVersion.getDescriptionShort(),
                      returnVersion::setDescriptionShort);
        copyIfChanged(newVersion.getDescriptionLong(), oldVersion.getDescriptionLong(),
                      returnVersion::setDescriptionLong);
        copyIfChanged(newVersion.getTableOfContents(), oldVersion.getTableOfContents(),
                      returnVersion::setTableOfContents);
        copyIfChanged(newVersion.getImageSmall(), oldVersion.getImageSmall(),
                      returnVersion::setImageSmall);
        copyIfChanged(newVersion.getImageOriginal(), oldVersion.getImageOriginal(),
                      returnVersion::setImageOriginal);
        copyIfChanged(newVersion.getImageLarge(), oldVersion.getImageLarge(),
                      returnVersion::setImageLarge);
        copyIfChanged(newVersion.getAudioFile(), oldVersion.getAudioFile(),
                      returnVersion::setAudioFile);

        return returnVersion;
    }

    private <T> void copyIfChanged(T newValue, T oldValue, Consumer<T> setter) {
        if (newValue != null && !newValue.equals(oldValue)) {
            setter.accept(newValue);
        }
    }

    /**
     * Method that writes a message to an already specified queue.
     *
     * @param message The message thats to be sent to the queue.
     * @throws SchedulerException when something goes wrong.
     */
    public void writeToDLQ(String message) throws SchedulerException {
        try {
            var sendMsgRequest = createSendMessageRequest(message);
            sqsClient.sendMessage(sendMsgRequest);
        } catch (UnsupportedOperationException e) {
            throw new SchedulerException("Failed to send message to DLQ. ", e);
        }
    }

    private SendMessageRequest createSendMessageRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(envHandler.readEnv(DLQ_QUEUE_URL_KEY))
                .messageBody(message)
                .delaySeconds(5)
                .build();
    }

}
