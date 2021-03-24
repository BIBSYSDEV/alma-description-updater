package no.unit.alma;

public class UpdatePayload {

    public String isbn;
    public String specifiedMaterial;
    public String url;


    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getSpecifiedMaterial() {
        return specifiedMaterial;
    }

    public void setSpecifiedMaterial(String specifiedMaterial) {
        this.specifiedMaterial = specifiedMaterial;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
