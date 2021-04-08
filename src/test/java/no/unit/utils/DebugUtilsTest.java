package no.unit.utils;

import no.unit.utils.DebugUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DebugUtilsTest {

    @Test
    public void testDumpException() {
        final RuntimeException runtimeException = new RuntimeException("RuntimeException");
        String exceptionDump = DebugUtils.dumpException(runtimeException);
        assertNotNull(exceptionDump);
    }

}