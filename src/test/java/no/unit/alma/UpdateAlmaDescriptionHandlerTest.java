package no.unit.alma;


import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpStatusCode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

public class UpdateAlmaDescriptionHandlerTest {

    Environment mockEnv;
    UpdateAlmaDescriptionHandler mockedHandler;

    private void initEnv() {
        when(mockEnv.readEnv("ALLOWED_ORIGIN")).thenReturn("Allow-origins");
        when(mockEnv.readEnv("ALMA_SRU_HOST")).thenReturn("Alma-sru-dot-com");
        when(mockEnv.readEnv("ALMA_API_HOST")).thenReturn("Alma-api-dpot-com");
    }

    /**
     * Sets up a mock environment for use when testing.
     */
    @BeforeEach
    public void init() {
        mockEnv = mock(Environment.class);
        initEnv();
        mockedHandler = new UpdateAlmaDescriptionHandler(mockEnv);
    }
    

    @Test
    public void testCreateGatewayResponse() {
        String successMessage = "Success";
        String failureMessage = "Failure";

        Map<String, Object> successResponse =
                mockedHandler.createGatewayResponse(HttpStatusCode.OK, successMessage, failureMessage);
        assertEquals(successMessage, successResponse.get(mockedHandler.RESPONSE_MESSAGE_KEY));
        Map<String, Object> failResponse =
                mockedHandler.createGatewayResponse(HttpStatusCode.BAD_REQUEST, successMessage, failureMessage);
        assertEquals(failureMessage, failResponse.get(mockedHandler.RESPONSE_MESSAGE_KEY));
    }

    @Test
    public void testCreateErrorResponse() {
        String errorMessage = "Error";
        int statusCode = 500;
        String actualErrorMessage = "{\"error\":\"Error\"}";
        GatewayResponse gatewayResponse = mockedHandler.createErrorResponse(errorMessage, statusCode);
        assertEquals(actualErrorMessage, gatewayResponse.getBody());
        assertEquals(statusCode, gatewayResponse.getStatusCode());
    }

    @Test
    public void testCheckProperties() throws Exception {
        assertTrue(mockedHandler.checkProperties());
    }

}