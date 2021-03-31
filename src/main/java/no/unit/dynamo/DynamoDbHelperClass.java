package no.unit.dynamo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamoDbHelperClass {

    private final String STANDARD_IMAGE_URL = "https://contents.bibsys.no/content/images/";
    private final String STANDARD_CONTENT_URL = "https://contents.bibsys.no/content/";


    private final String SMALL_KEY = "small";
    private final String LARGE_KEY = "large";
    private final String ORIGINAL_KEY = "original";
    private final String SMALL_DESCRIPTION = "Miniatyrbilde";
    private final String LARGE_DESCRIPTION = "Omslagsbilde";
    private final String ORIGINAL_DESCRIPTION = "Originalt bilde";
    private final String SHORT_KEY = "description_short";
    private final String LONG_KEY = "description_long";
    private final String CONTENTS_KEY = "contents";
    private final String SHORT_DESCRIPTION = "Forlagets beskrivelse (kort)";
    private final String LONG_DESCRIPTION = "Forlagets beskrivelse (lang)";
    private final String CONTENTS_DESCRIPTION = "Innholdsfortegnelse";


    public List<UpdatePayload> createLinks(List<DynamoDbItem> items) {
        List<UpdatePayload> payloads = new ArrayList<>();
        for(DynamoDbItem item : items){
            if(item.getDescription_short() != null) payloads
                    .add(createContentLink(SHORT_KEY, item.getIsbn()));
            if(item.getDescription_long() != null) payloads
                    .add(createContentLink(LONG_KEY, item.getIsbn()));
            if(item.getTable_of_contents() != null) payloads
                    .add(createContentLink(CONTENTS_KEY, item.getIsbn()));
            if(item.getImage_small() != null) payloads
                    .add(createImageLink(SMALL_KEY, item.getIsbn()));
            if(item.getImage_large() != null) payloads
                    .add(createImageLink(LARGE_KEY, item.getIsbn()));
            if(item.getImage_original() != null) payloads
                    .add(createImageLink(ORIGINAL_KEY, item.getIsbn()));
        }
        return payloads;
    }

    public UpdatePayload createImageLink(String imageSize, String isbn) {
        UpdatePayload payload = new UpdatePayload();
        String secondLinkPart = isbn.substring(isbn.length() - 2, isbn.length() - 1);
        String firstLinkPart = isbn.substring(isbn.length() - 1);
        String link = String.format(STANDARD_IMAGE_URL + imageSize + "/%s/%s/%s.jpg", firstLinkPart, secondLinkPart, isbn);
        String specifiedMaterial;
        switch (imageSize){
            case(SMALL_KEY):
                specifiedMaterial = SMALL_DESCRIPTION;
                break;
            case(LARGE_KEY):
                specifiedMaterial = LARGE_DESCRIPTION;
                break;
            default:
                specifiedMaterial = ORIGINAL_DESCRIPTION;
        }
        payload.setIsbn(isbn);
        payload.setLink(link);
        payload.setSpecifiedMaterial(specifiedMaterial);
        return payload;
    }

    public UpdatePayload createContentLink(String contentType, String isbn) {
        UpdatePayload payload = new UpdatePayload();
        String link = String.format(STANDARD_CONTENT_URL + isbn + "?type=" + contentType.toUpperCase());
        String specifiedMaterial;
        switch (contentType.toLowerCase()){
            case(SHORT_KEY):
                specifiedMaterial = SHORT_DESCRIPTION;
                break;
            case(LONG_KEY):
                specifiedMaterial = LONG_DESCRIPTION;
                break;
            default:
                specifiedMaterial = CONTENTS_DESCRIPTION;
        }
        payload.setIsbn(isbn);
        payload.setLink(link);
        payload.setSpecifiedMaterial(specifiedMaterial);
        return payload;
    }

    public String getDateAsString(){
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        System.out.println(now);
        System.out.println(yesterday);
        return yesterday.toString();
    }
}
