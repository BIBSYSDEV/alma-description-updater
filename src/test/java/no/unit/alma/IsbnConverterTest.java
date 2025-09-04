package no.unit.alma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class IsbnConverterTest {

    private static final String ISBN13 = "9780198242833";
    private static final String ISBN10 = "0198242832";

    IsbnConverter isbnConverter = new IsbnConverter();

    @Test
    public void testConvertIsbn() {
        assertEquals("0201882957", isbnConverter.convertIsbn("9780201882957"));
        assertEquals("9780201882957", isbnConverter.convertIsbn("0201882957"));
        assertEquals("1292101768", isbnConverter.convert13To10("9781292101767"));
        assertEquals("9780345391803", isbnConverter.convert10To13("0345391802"));
        assertEquals("9780345391810", isbnConverter.convert10To13("0345391810"));
        assertEquals("074754624X", isbnConverter.convert13To10("9780747546245"));
        assertEquals("0345391810", isbnConverter.convert13To10("9780345391810"));
    }

}