package no.unit.dynamo;

public class UpdatePayload {

    private String isbn;
    private String link;
    private String specifiedMaterial;

    public String getIsbn() {
        return isbn;
    }

    public String getLink() {
        return link;
    }

    public String getSpecifiedMaterial() {
        return specifiedMaterial;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setSpecifiedMaterial(String specifiedMaterial) {
        this.specifiedMaterial = specifiedMaterial;
    }

    public String toString() {
        return "ISBN: " + isbn + "\nLink: " + link + "\nSpecifiedMaterial: " + specifiedMaterial;
    }
}
