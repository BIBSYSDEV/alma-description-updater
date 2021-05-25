package no.unit.alma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlmaHelperTest {

    private static final String ISBN13 = "9780198242833";
    private static final String ISBN10 = "0198242832";

    AlmaHelper almaHelper = new AlmaHelper();

    @Test
    public void testConvertIsbn() {
        assertEquals("0201882957", almaHelper.convertIsbn("9780201882957"));
        assertEquals("9780201882957", almaHelper.convertIsbn("0201882957"));
        assertEquals("1292101768", almaHelper.convert13To10("9781292101767" ));
        assertEquals("9780345391803" , almaHelper.convert10To13("0345391802"));
    }

}