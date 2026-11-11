package no.unit.scheduler;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class UpdateItem {

    private String isbn;
    private String link;
    private String specifiedMaterial;

    public UpdateItem() {

    }

    public UpdateItem(String isbn, String link, String specifiedMaterial) {
        this.isbn = isbn;
        this.link = link;
        this.specifiedMaterial = specifiedMaterial;
    }

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpdateItem that = (UpdateItem) o;
        return Objects.equals(getIsbn(), that.getIsbn())
               && Objects.equals(getLink(), that.getLink())
               && Objects.equals(getSpecifiedMaterial(), that.getSpecifiedMaterial());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIsbn(), getLink(), getSpecifiedMaterial());
    }

}
