package no.unit.dynamo;

import nva.commons.utils.JacocoGenerated;

@JacocoGenerated
public class DynamoDbItem {

    private transient String isbn;
    private transient String title;
    private transient String created;
    private transient String source;
    private transient String descriptionShort;
    private transient String dateOfPublication;
    private transient String imageLarge;
    private transient String descriptionLong;
    private transient String modified;
    private transient String imageSmall;
    private transient String tableOfContents;
    private transient String author;
    private transient String imageOriginal;

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
    public String getDescriptionShort() {
        return descriptionShort;
    }

    @JacocoGenerated
    public String getDateOfPublication() {
        return dateOfPublication;
    }

    @JacocoGenerated
    public String getImageLarge() {
        return imageLarge;
    }

    @JacocoGenerated
    public String getDescriptionLong() {
        return descriptionLong;
    }

    @JacocoGenerated
    public String getModified() {
        return modified;
    }

    @JacocoGenerated
    public String getImageSmall() {
        return imageSmall;
    }

    @JacocoGenerated
    public String getTableOfContents() {
        return tableOfContents;
    }

    @JacocoGenerated
    public String getAuthor() {
        return author;
    }

    @JacocoGenerated
    public String getImageOriginal() {
        return imageOriginal;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public void setTitle(String title) { this.title = title; }

    public void setCreated(String created) { this.created = created; }

    public void setSource(String source) { this.source = source; }

    public void setDescriptionShort(String descriptionShort) {
        this.descriptionShort = descriptionShort;
    }

    public void setDateOfPublication(String dateOfPublication) { this.dateOfPublication = dateOfPublication; }

    public void setImageLarge(String imageLarge) {
        this.imageLarge = imageLarge;
    }

    public void setDescriptionLong(String descriptionLong) {
        this.descriptionLong = descriptionLong;
    }

    public void setModified(String modified) { this.modified = modified; }

    public void setImageSmall(String imageSmall) {
        this.imageSmall = imageSmall;
    }

    public void setTableOfContents(String tableOfContents) {
        this.tableOfContents = tableOfContents;
    }

    public void setAuthor(String author) { this.author = author; }

    public void setImageOriginal(String imageOriginal) {
        this.imageOriginal = imageOriginal;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return "isbn: " + isbn + "\ntitle: " + title + "\ncreated: " + created + "\nsource: "
                + source + "\ndescShort: " + descriptionShort + "\nDate_publication: "
                + dateOfPublication + "\nimage_large: " + imageLarge + "\nDescLong: "
                + descriptionLong + "\nmodified: " + modified + "\nimage_small: "
                + imageSmall + "\ncontents: " + tableOfContents + "\nauthor: " + author
                + "\nimage_original: " + imageOriginal;
    }

}
