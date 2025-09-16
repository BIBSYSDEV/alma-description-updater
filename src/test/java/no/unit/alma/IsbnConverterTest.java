package no.unit.alma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IsbnConverterTest {

    private static final String ISBN13 = "9780198242833";
    private static final String ISBN10 = "0198242832";

    private static IsbnConverter isbnConverter;

    @BeforeAll
    static void setUp() {
        isbnConverter = new IsbnConverter();
    }

    @Test
    public void shouldConvertIsbn10To13() {
        assertEquals("9780201882957", isbnConverter.convertIsbn("0201882957"));
        assertEquals("9780345391803", isbnConverter.convertIsbn("0345391802"));
        assertEquals("9780345391810", isbnConverter.convertIsbn("0345391810"));
    }

    @Test
    public void shouldConvertIsbn13To10IncludingXIsbn10() {
        assertEquals("0201882957", isbnConverter.convertIsbn("9780201882957"));
        assertEquals("1292101768", isbnConverter.convertIsbn("9781292101767"));
        assertEquals("074754624X", isbnConverter.convertIsbn("9780747546245"));
        assertEquals("0345391810", isbnConverter.convertIsbn("9780345391810"));
        assertEquals("8210053418", isbnConverter.convertIsbn("9788210053412"));
    }

    @Test
    public void shouldConvertIsbnBothWaysBetweenIsbn10AndIsbn13() {
        assertEquals(ISBN10, isbnConverter.convertIsbn(ISBN13));
        assertEquals(ISBN13, isbnConverter.convertIsbn(ISBN10));
    }

}
