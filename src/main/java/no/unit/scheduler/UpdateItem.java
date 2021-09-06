package no.unit.scheduler;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class UpdateItem {

    private String isbn;
    private String link;
    private String specifiedMaterial;

    @JacocoGenerated
    public String getIsbn() {
        return isbn;
    }

    @JacocoGenerated
    public String getLink() {
        return link;
    }

    @JacocoGenerated
    public String getSpecifiedMaterial() {
        return specifiedMaterial;
    }

    @JacocoGenerated
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    @JacocoGenerated
    public void setLink(String link) {
        this.link = link;
    }

    @JacocoGenerated
    public void setSpecifiedMaterial(String specifiedMaterial) {
        this.specifiedMaterial = specifiedMaterial;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return "ISBN: " + isbn + "\nLink: " + link + "\nSpecifiedMaterial: " + specifiedMaterial;
    }
}
