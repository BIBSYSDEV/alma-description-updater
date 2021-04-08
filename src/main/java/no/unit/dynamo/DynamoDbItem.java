package no.unit.dynamo;

import nva.commons.utils.JacocoGenerated;

@JacocoGenerated
public class DynamoDbItem {

    private String isbn;
    private String title;
    private String created;
    private String source;
    private String description_short;
    private String date_of_publication;
    private String image_large;
    private String description_long;
    private String modified;
    private String image_small;
    private String table_of_contents;
    private String author;
    private String image_original;

    @JacocoGenerated
    public String getIsbn() {
        return isbn;
    }

    @JacocoGenerated
    public String getTitle() {
        return title;
    }

    @JacocoGenerated
    public String getCreated() {
        return created;
    }

    @JacocoGenerated
    public String getSource() {
        return source;
    }

    @JacocoGenerated
    public String getDescription_short() {
        return description_short;
    }

    @JacocoGenerated
    public String getDate_of_publication() {
        return date_of_publication;
    }

    @JacocoGenerated
    public String getImage_large() {
        return image_large;
    }

    @JacocoGenerated
    public String getDescription_long() {
        return description_long;
    }

    @JacocoGenerated
    public String getModified() {
        return modified;
    }

    @JacocoGenerated
    public String getImage_small() {
        return image_small;
    }

    @JacocoGenerated
    public String getTable_of_contents() {
        return table_of_contents;
    }

    @JacocoGenerated
    public String getAuthor() {
        return author;
    }

    @JacocoGenerated
    public String getImage_original() {
        return image_original;
    }

    @JacocoGenerated
    public String toString() {
        return "isbn: " + isbn + "\ntitle: " + title + "\ncreated: " + created + "\nsource: "
                + source + "\ndescShort: " + description_short + "\nDate_publication: "
                + date_of_publication + "\nimage_large: " + image_large + "\nDescLong: "
                + description_long + "\nmodified: " + modified + "\nimage_small: "
                + image_small + "\ncontents: " + table_of_contents + "\nauthor: " + author
                + "\nimage_original: " + image_original;
    }

}
