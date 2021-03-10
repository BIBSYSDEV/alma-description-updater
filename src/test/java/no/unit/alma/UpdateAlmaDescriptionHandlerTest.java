package no.unit.alma;


import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.core.Response;

public class UpdateAlmaDescriptionHandlerTest {


    @Test
    public void testMissingParameters() {
        Config.getInstance().setAlmaSruEndpoint("ALMA_SRU_HOST");
        UpdateAlmaDescriptionHandler mockUpdateAlmaHandler = new UpdateAlmaDescriptionHandler();

        GatewayResponse result = mockUpdateAlmaHandler.handleRequest(null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
        assertTrue(result.getBody().contains(mockUpdateAlmaHandler.MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS));

        Map<String, Object> event = new HashMap<>();
        result = mockUpdateAlmaHandler.handleRequest(event, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
        assertTrue(result.getBody().contains(UpdateAlmaDescriptionHandler.MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS));

        event.put(UpdateAlmaDescriptionHandler.QUERY_STRING_PARAMETERS_KEY, null);
        result = mockUpdateAlmaHandler.handleRequest(event, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
        assertTrue(result.getBody().contains(UpdateAlmaDescriptionHandler.MISSING_EVENT_ELEMENT_QUERYSTRINGPARAMETERS));

        Map<String, String> queryParameters = new HashMap<>();
        event.put(UpdateAlmaDescriptionHandler.QUERY_STRING_PARAMETERS_KEY, queryParameters);
        result = mockUpdateAlmaHandler.handleRequest(event, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
        assertTrue(result.getBody().contains(UpdateAlmaDescriptionHandler.MANDATORY_PARAMETER_MISSING));
    }

    @Test
    public void testCreateGatewayResponse() {
        UpdateAlmaDescriptionHandler handler = new UpdateAlmaDescriptionHandler();
        boolean condition = true;
        String successMessage = "Success";
        String failureMessage = "Failure";

        Map<String, Object> successResponse = handler.createGatewayResponse(condition, successMessage, failureMessage);
        assertEquals(successMessage, successResponse.get(handler.RESPONSE_MESSAGE_KEY));
        Map<String, Object> failResponse = handler.createGatewayResponse(!condition, successMessage, failureMessage);
        assertEquals(failureMessage, failResponse.get(handler.RESPONSE_MESSAGE_KEY));
    }

    @Test
    public void testCreateErrorResponse() {
        UpdateAlmaDescriptionHandler handler = new UpdateAlmaDescriptionHandler();
        String errorMessage = "Error";
        int statusCode = 500;
        String actualErrorMessage = "{\"error\":\"Error\"}";
        GatewayResponse gatewayResponse = handler.createErrorResponse(errorMessage, statusCode);
        assertEquals(actualErrorMessage, gatewayResponse.getBody());
        assertEquals(statusCode, gatewayResponse.getStatusCode());
    }

    @Test
    public void testFullRun(){
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("isbn", "9788210053412");
        queryParameters.put("description", "This is a test 3");
        queryParameters.put("url", "this/is/a/test/3");
        Map<String, Object> event = new HashMap<>();
        event.put("queryStringParameters", queryParameters);

        Config.getInstance().setCorsHeader("*");

        UpdateAlmaDescriptionHandler updateHandler = new UpdateAlmaDescriptionHandler();

        GatewayResponse response = updateHandler.handleRequest(event, null);

        System.out.println(response.getBody());

        assertEquals(200, response.getStatusCode());
    }

}