package no.unit.alma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretRetrieverTest {

    @Test
    public void testGetSecret(){
        SecretRetriever sr = new SecretRetriever();
        assertNotNull(sr.getSecret());
    }

}