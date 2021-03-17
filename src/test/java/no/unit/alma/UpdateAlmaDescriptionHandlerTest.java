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
    public void testMissingParameters() {
        GatewayResponse result = mockedHandler.handleRequest(null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
        assertTrue(result.getBody().contains(mockedHandler.MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS));

        Map<String, Object> event = new HashMap<>();
        result = mockedHandler.handleRequest(event, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
        assertTrue(result.getBody().contains(UpdateAlmaDescriptionHandler.MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS));

        event.put(UpdateAlmaDescriptionHandler.QUERY_STRING_PARAMETERS_KEY, null);
        result = mockedHandler.handleRequest(event, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
        assertTrue(result.getBody().contains(UpdateAlmaDescriptionHandler.MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS));

        Map<String, String> queryParameters = new HashMap<>();
        event.put(UpdateAlmaDescriptionHandler.QUERY_STRING_PARAMETERS_KEY, queryParameters);
        result = mockedHandler.handleRequest(event, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
        assertTrue(result.getBody().contains(UpdateAlmaDescriptionHandler.MANDATORY_PARAMETER_MISSING));
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