package no.unit.alma;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    public void testCheckPropertiesNothingSet() {
        final Config config = Config.getInstance();
        config.setAlmaSruEndpoint(null);
        Assertions.assertThrows(RuntimeException.class, config::checkProperties);
    }

    @Test
    public void testCorsHeaderNotSet() {
        final Config config = Config.getInstance();
        config.setCorsHeader(null);
        final String corsHeader = config.getCorsHeader();
        assertNull(corsHeader);
    }

    @Test
    public void testCheckPropertiesSet() {
        final Config instance = Config.getInstance();
        instance.setAlmaSruEndpoint(Config.ALMA_SRU_HOST_KEY);
        assertTrue(instance.checkProperties());
        assertEquals(Config.ALMA_SRU_HOST_KEY, instance.getAlmaSruEndpoint());
    }

}