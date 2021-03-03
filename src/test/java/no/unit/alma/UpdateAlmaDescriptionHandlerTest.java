package no.unit.alma;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import javax.ws.rs.core.Response;

public class UpdateAlmaDescriptionHandlerTest {


    @Test
    public void testMissingParameters() throws Exception{
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

}