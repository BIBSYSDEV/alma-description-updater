package no.unit.alma;

public class IsbnConverter {

    private static final String TAG_978 = "978";
    private static final int TAG_11 = 11;
    private static final int TAG_10 = 10;

    /**
     * Method for converting ISBN to 10 or 13.
     * @param isbn The isbn to be converted.
     * @return Returns an isbn in the opposite format.
     */
    public String convertIsbn(String isbn) {
        String convertedIsbn;
        if (isbn.length() > TAG_11) {
            convertedIsbn = convert13To10(isbn);
        } else {
            convertedIsbn = convert10To13(isbn);
        }
        return convertedIsbn;
    }

    /**
     * Converts isbn10 to isbn13.
     * @param isbn10 the isbn to be converted.
     * @return the isbn13.
     */
    private String convert10To13(String isbn10) {
        String isbn = TAG_978 + isbn10.substring(0, isbn10.length() - 1);
        int sum = 0;
        int mulitiplier;
        for (int i = 0; i < isbn.length(); i++) {
            mulitiplier = (i % 2 == 0) ? 1 : 3;
            sum += Character.getNumericValue(isbn.charAt(i)) * mulitiplier;
        }

        int moduloResult = sum % 10;
        int lastDigit = 0;
        if (moduloResult != 0) {
            lastDigit = 10 - moduloResult;
        }
        String isbn13 = isbn + lastDigit;
        return isbn13;
    }

    /**
     * Converts isbn13 to isbn10.
     * @param isbn13 the isbn to be converted.
     * @return the isbn10.
     */
    private String convert13To10(String isbn13) {
        String isbn = isbn13.substring(3, isbn13.length() - 1);
        int sum = 0;
        for (int i = 0; i < isbn.length(); i++) {
            sum += Character.getNumericValue(isbn.charAt(i)) * (10 - i);
        }

        int checksum = 11 - (sum % 11);
        String lastDigit;
        if (checksum == TAG_10) {
            lastDigit = "X";
        } else if (checksum == TAG_11) {
            lastDigit = "0";
        } else {
            lastDigit = String.valueOf(checksum);
        }
        String isbn10 = isbn + lastDigit;
        return isbn10;
    }

}
