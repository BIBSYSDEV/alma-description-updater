package no.unit.dynamo;

import no.unit.exceptions.DynamoDbException;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DynamoDbHelperClass {

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
    private static final String SHORT_DESCRIPTION = "Forlagets beskrivelse (kort)";
    private static final String LONG_DESCRIPTION = "Forlagets beskrivelse (lang)";
    private static final String CONTENTS_DESCRIPTION = "Innholdsfortegnelse";

    private final transient Environment envHandler;


    public DynamoDbHelperClass(Environment envHandler) {
        this.envHandler = envHandler;
    }

    public DynamoDbHelperClass() {
        this.envHandler = new Environment();
    }

    /**
     * Creates a list of UpdatePayload items based on a list of DynamoDbItems.
     *     Each DynamoDbItem may result in several UpdatePayload items.
     * @param items The list which to extract and create UpdatePayload items from.
     * @return A list of UpdatePayload items.
     * @throws DynamoDbException When something goes wrong.
     */
    public List<UpdatePayload> createLinks(List<DynamoDbItem> items) throws DynamoDbException {
        List<UpdatePayload> payloads = new ArrayList<>();
        for (DynamoDbItem item : items) {
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
     * Returns the full date exactly 24 hours ago as a string.
     * @return The date 24 hours ago.
     */
    @JacocoGenerated
    public String getYesterdaysDate() {
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        return yesterday.toString();
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
        if (!newVersion.getDescriptionShort().equals(oldVersion.getDescriptionShort())) {
            returnVersion.setDescriptionShort(newVersion.getDescriptionShort());
        }
        if (!newVersion.getDescriptionLong().equals(oldVersion.getDescriptionLong())) {
            returnVersion.setDescriptionLong(newVersion.getDescriptionLong());
        }
        if (!newVersion.getTableOfContents().equals(oldVersion.getTableOfContents())) {
            returnVersion.setTableOfContents(newVersion.getTableOfContents());
        }
        if (!newVersion.getImageSmall().equals(oldVersion.getImageSmall())) {
            returnVersion.setImageSmall(newVersion.getImageSmall());
        }
        if (!newVersion.getImageOriginal().equals(oldVersion.getImageOriginal())) {
            returnVersion.setImageOriginal(newVersion.getImageOriginal());
        }
        if (!newVersion.getImageLarge().equals(oldVersion.getImageLarge())) {
            returnVersion.setImageLarge(newVersion.getImageLarge());
        }
        return returnVersion;
    }
}
