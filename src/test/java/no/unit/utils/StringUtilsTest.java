package no.unit.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilsTest {

    @Test
    void testIsEmpty() {
        String emptyString = "";
        assertTrue(StringUtils.isEmpty(emptyString));
    }

    @Test
    void testIsNotEmpty() {
        String notEmptyString = "Not an empty String";
        assertTrue(StringUtils.isNotEmpty(notEmptyString));
    }
}