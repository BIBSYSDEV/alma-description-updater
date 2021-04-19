package no.unit.dynamo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import no.unit.exceptions.DynamoDbException;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DynamoDbHelper {

    private static final String IMAGE_URL_KEY = "STANDARD_IMAGE_URL";
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
    public static final String S = "S";
    private static final String SHORT_DESCRIPTION = "Forlagets beskrivelse (kort)";
    private static final String LONG_DESCRIPTION = "Forlagets beskrivelse (lang)";
    private static final String CONTENTS_DESCRIPTION = "Innholdsfortegnelse";

    private final transient Environment envHandler;


    public DynamoDbHelper(Environment envHandler) {
        this.envHandler = envHandler;
    }

    public DynamoDbHelper() {
        this.envHandler = new Environment();
    }


    /**
     * Creates a list of UpdatePayload items, if the event is "MODIFIED" this will be from the
     *     difference between the new and the old image, if created simply from the new image.
     * @param eventBody The body of the SQSEvent.
     * @return A list of UpdatePayload objects.
     * @throws Exception when something goes wrong.
     */
    public List<UpdatePayload> splitEventIntoUpdatePayloads(String eventBody) throws Exception {
        JsonObject eventBodyObject = JsonParser.parseString(eventBody).getAsJsonObject();
        String isbn = eventBodyObject.get("dynamodb").getAsJsonObject().get("Keys")
                .getAsJsonObject().get("isbn").getAsJsonObject().get(S).getAsString();
        String eventName = eventBodyObject.get("eventName").getAsString();
        JsonObject newImage = eventBodyObject.get("dynamodb").getAsJsonObject().get("NewImage").getAsJsonObject();
        DynamoDbItem newDynamoItem = extractFromJsonObject(newImage);
        newDynamoItem.setIsbn(isbn);
        if (eventName.equals("MODIFY")) {
            JsonObject oldImage = eventBodyObject.get("dynamodb").getAsJsonObject().get("OldImage").getAsJsonObject();
            DynamoDbItem oldDynamoItem = extractFromJsonObject(oldImage);
            oldDynamoItem.setIsbn(isbn);

            DynamoDbItem diffDynamoItem = extractDiffs(newDynamoItem, oldDynamoItem);

            return createLinks(diffDynamoItem);
        } else {
            return createLinks(newDynamoItem);
        }
    }

    /**
     * Extracts the data needed to create an DynamoDbItem from a JsonObject.
     * @param image Either the new or the old image, containing the dynamoDbItem.
     * @return The DynamoDbItem.
     * @throws Exception when something goes wrong.
     */
    private DynamoDbItem extractFromJsonObject(JsonObject image) throws Exception {
        DynamoDbItem dynamoItem = new DynamoDbItem();
        dynamoItem.setDescriptionShort((image.get("description_short") == null) ? null : image.get("description_short")
                .getAsJsonObject().get(S).getAsString());
        dynamoItem.setImageLarge((image.get("image_large") == null) ? null : image.get("image_large").getAsJsonObject().get(S).getAsString());
        dynamoItem.setDescriptionLong((image.get("description_long") == null) ? null : image.get("description_long")
                .getAsJsonObject().get(S).getAsString());
        dynamoItem.setImageSmall((image.get("image_small") == null) ? null : image.get("image_small").getAsJsonObject().get(S).getAsString());
        dynamoItem.setTableOfContents((image.get("table_of_contents") == null) ? null : image.get("table_of_contents")
                .getAsJsonObject().get(S).getAsString());
        dynamoItem.setImageOriginal((image.get("image_original") == null) ? null : image.get("image_original").getAsJsonObject().get(S).getAsString());

        return dynamoItem;
    }

    /**
     * Creates a list of UpdatePayload items based on a list of DynamoDbItems.
     *     The DynamoDbItem may result in several UpdatePayload items.
     * @param item The DynamoDbItem from which to extract and create UpdatePayload items from.
     * @return A list of UpdatePayload items.
     * @throws DynamoDbException When something goes wrong.
     */
    public List<UpdatePayload> createLinks(DynamoDbItem item) throws DynamoDbException {
        List<UpdatePayload> payloads = new ArrayList<>();
        if (item.getDescriptionShort() != null) {
            payloads.add(createContentLink(SHORT_KEY, item.getIsbn()));
        }
        if (item.getDescriptionLong() != null) {
            payloads.add(createContentLink(LONG_KEY, item.getIsbn()));
        }
        if (item.getTableOfContents() != null) {
            payloads.add(createContentLink(CONTENTS_KEY, item.getIsbn()));
        }
        if (item.getImageSmall() != null) {
            payloads.add(createImageLink(SMALL_KEY, item.getIsbn()));
        }
        if (item.getImageLarge() != null) {
            payloads.add(createImageLink(LARGE_KEY, item.getIsbn()));
        }
        if (item.getImageOriginal() != null) {
            payloads.add(createImageLink(ORIGINAL_KEY, item.getIsbn()));
        }

        return payloads;
    }

    /**
     * Creates a UpdatePayload Item containing the correct isbn, link and specifiedMaterial.
     * @param imageSize The size of the Image to create a link for.
     * @param isbn The isbn to create the UpdatePayload item for.
     * @return A UpdatePayload item.
     * @throws DynamoDbException When the environment variable hasn't been set.
     */
    public UpdatePayload createImageLink(String imageSize, String isbn) throws DynamoDbException {
        String link = "";
        String secondLinkPart = isbn.substring(isbn.length() - 2, isbn.length() - 1);
        String firstLinkPart = isbn.substring(isbn.length() - 1);
        try {
            link = String.format(envHandler.readEnv(IMAGE_URL_KEY) + imageSize
                    + "/%s/%s/%s.jpg", firstLinkPart, secondLinkPart, isbn);
        } catch (IllegalStateException e) {
            throw new DynamoDbException("No env-variable set for " + IMAGE_URL_KEY, e);
        }
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
        UpdatePayload payload = new UpdatePayload();
        payload.setIsbn(isbn);
        payload.setLink(link);
        payload.setSpecifiedMaterial(specifiedMaterial);
        return payload;
    }

    /**
     * Creates a UpdatePayload Item containing the correct isbn, link and specifiedMaterial.
     * @param contentType The type of content to create a link for.
     * @param isbn The isbn to create the UpdatePayload item for.
     * @return A UpdatePayload item.
     * @throws DynamoDbException When the environment variable hasn't been set.
     */
    public UpdatePayload createContentLink(String contentType, String isbn) throws DynamoDbException {
        String link = "";
        try {
            link = envHandler.readEnv(CONTENT_URL_KEY) + isbn + "?type=" + contentType.toUpperCase(Locale.getDefault());
        } catch (IllegalStateException e) {
            throw new DynamoDbException("No env-variable set for " + CONTENT_URL_KEY, e);
        }
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
        UpdatePayload payload = new UpdatePayload();
        payload.setIsbn(isbn);
        payload.setLink(link);
        payload.setSpecifiedMaterial(specifiedMaterial);
        return payload;
    }

    /**
     * Method to fill the actually updated fields of a DynamoDbItem.
     * @param newVersion DynamoDbItem containing the new version of the db-record.
     * @param oldVersion DynamoDbItem containing the old version of the db-record.
     * @return A DynamoDbItem with only the field of interest filed.
     */
    public DynamoDbItem extractDiffs(DynamoDbItem newVersion, DynamoDbItem oldVersion) {
        DynamoDbItem returnVersion = new DynamoDbItem();
        returnVersion.setIsbn(newVersion.getIsbn());
        if ( newVersion.getDescriptionShort() != null && oldVersion.getDescriptionShort() != null &&
                !newVersion.getDescriptionShort().equals(oldVersion.getDescriptionShort())) {
            returnVersion.setDescriptionShort(newVersion.getDescriptionShort());
        }
        if (newVersion.getDescriptionLong() != null && oldVersion.getDescriptionLong() != null &&
                !newVersion.getDescriptionLong().equals(oldVersion.getDescriptionLong())) {
            returnVersion.setDescriptionLong(newVersion.getDescriptionLong());
        }
        if (newVersion.getTableOfContents() != null && oldVersion.getTableOfContents() != null &&
                !newVersion.getTableOfContents().equals(oldVersion.getTableOfContents())) {
            returnVersion.setTableOfContents(newVersion.getTableOfContents());
        }
        if (newVersion.getImageSmall() != null && oldVersion.getImageSmall() != null &&
                !newVersion.getImageSmall().equals(oldVersion.getImageSmall())) {
            returnVersion.setImageSmall(newVersion.getImageSmall());
        }
        if (newVersion.getImageOriginal() != null && oldVersion.getImageOriginal() != null &&
                !newVersion.getImageOriginal().equals(oldVersion.getImageOriginal())) {
            returnVersion.setImageOriginal(newVersion.getImageOriginal());
        }
        if (newVersion.getImageLarge() != null && oldVersion.getImageLarge() != null &&
                !newVersion.getImageLarge().equals(oldVersion.getImageLarge())) {
            returnVersion.setImageLarge(newVersion.getImageLarge());
        }
        return returnVersion;
    }
}
