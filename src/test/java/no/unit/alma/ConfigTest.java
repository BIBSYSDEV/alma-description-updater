package no.unit.alma;

import static no.unit.alma.Config.ALMA_API_HOST_KEY;
import static no.unit.alma.Config.ALMA_API_KEY;
import static no.unit.alma.Config.ALMA_SRU_HOST_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigTest {

    public static final String ERROR_MESSAGE = "Error while setting up env-variables and secretKeys. Failed to "
                                               + "initialize variables. ";
    private Environment mockEnv;

    @BeforeEach
    public void setup() {
        mockEnv = mock(Environment.class);
    }

    @Test
    public void shouldInitEnvironmentCorrectly() {
        doReturn("alma_host").when(mockEnv).readEnv(ALMA_API_HOST_KEY);
        doReturn("alma_sru_host").when(mockEnv).readEnv(ALMA_SRU_HOST_KEY);
        doReturn("alma_api_key").when(mockEnv).readEnv(ALMA_API_KEY);

        var config = new Config(mockEnv);

        assertThat(config.almaApiHost, equalTo("alma_host"));
        assertThat(config.almaSruHost, equalTo("alma_sru_host"));
        assertThat(config.secretKey, equalTo("alma_api_key"));
    }

    @Test
    public void shouldHandleAndThrowCorrectException() {
        doThrow(new IllegalStateException()).when(mockEnv).readEnv(ALMA_API_HOST_KEY);

        var exception = assertThrows(RuntimeException.class, () -> new Config(mockEnv));

        assertThat(exception.getMessage(), equalTo(ERROR_MESSAGE));
    }

}