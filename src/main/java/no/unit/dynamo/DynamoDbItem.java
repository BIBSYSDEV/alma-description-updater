package no.unit.dynamo;

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

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getCreated() {
        return created;
    }

    public String getSource() {
        return source;
    }

    public String getDescription_short() {
        return description_short;
    }

    public String getDate_of_publication() {
        return date_of_publication;
    }

    public String getImage_large() {
        return image_large;
    }

    public String getDescription_long() {
        return description_long;
    }

    public String getModified() {
        return modified;
    }

    public String getImage_small() {
        return image_small;
    }

    public String getTable_of_contents() {
        return table_of_contents;
    }

    public String getAuthor() {
        return author;
    }

    public String getImage_original() {
        return image_original;
    }

    public String toString() {
        return "isbn: " + isbn + "\ntitle: " + title + "\ncreated: " + created + "\nsource: "
                + source + "\ndescShort: " + description_short + "\nDate_publication: "
                + date_of_publication + "\nimage_large: " + image_large + "\nDescLong: "
                + description_long + "\nmodified: " + modified + "\nimage_small: "
                + image_small + "\ncontents: " + table_of_contents + "\nauthor: " + author
                + "\nimage_original: " + image_original;
    }

}
