package no.unit.alma;

import com.amazonaws.services.dynamodbv2.xspec.S;
import nva.commons.utils.Environment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class GatewayResponseTest {

    private static final String EMPTY_STRING = "";
    public static final String CORS_HEADER = "CORS header";
    public static final String MOCK_BODY = "mock";
    public static final String ERROR_BODY = "error";
    public static final String ERROR_JSON = "{\"error\":\"error\"}";
    public static final String FIRST_PART_OF_STRING = "First part of string, ";
    public static final String SECOND_PART_OF_STRING = "Second part of string";
    Environment mockEnv;
    GatewayResponse mockedGR;

    private void initEnv() {
        when(mockEnv.readEnv("ALLOWED_ORIGIN")).thenReturn("Allow-origins");
        when(mockEnv.readEnv("ALMA_SRU_HOST")).thenReturn("Alma-sru-dot-com");
        when(mockEnv.readEnv("ALMA_API_HOST")).thenReturn("Alma-api-dpot-com");
    }

    @BeforeEach
    public void init() {
        mockEnv = mock(Environment.class);
        initEnv();
        mockedGR = new GatewayResponse(mockEnv);
    }

    @Test
    public void testErrorResponse() {
        mockedGR.setBody(null);
        mockedGR.setErrorBody(ERROR_BODY);
        mockedGR.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
        Assertions.assertEquals(ERROR_JSON, mockedGR.getBody());
    }

    @Test
    public void testSuccessResponse() {
        mockedGR.setBody(MOCK_BODY);
        mockedGR.setStatusCode(Response.Status.OK.getStatusCode());
        Assertions.assertEquals(MOCK_BODY, mockedGR.getBody());
    }

    @Test
    public void testAppendBody() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(FIRST_PART_OF_STRING);
        stringBuilder.append(SECOND_PART_OF_STRING);
        GatewayResponse response = new GatewayResponse();
        response.setBody(FIRST_PART_OF_STRING);
        response.appendBody(SECOND_PART_OF_STRING);
        Assertions.assertEquals(stringBuilder.toString(), response.getBody());
    }

}