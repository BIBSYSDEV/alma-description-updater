package no.unit.alma;

import com.google.gson.JsonObject;
import no.unit.utils.StringUtils;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * POJO containing response object for API Gateway.
 */
public class GatewayResponse {

    public static final String CORS_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    public static final String ALLOWED_ORIGIN_KEY = "ALLOWED_ORIGIN";
    public static final String EMPTY_JSON = "{}";
    public static final transient String ERROR_KEY = "error";
    private String body;
    private transient Map<String, String> headers;
    private int statusCode;
    private final transient Environment envHandler;

    /**
     * Constructor that accepts an Environment variable, mainly used for testing.
     * @param envHandler the Environment variable to be submitted to the constructor.
     */
    public GatewayResponse(Environment envHandler) {
        this.envHandler = envHandler;
        this.statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        this.body = EMPTY_JSON;
        this.generateDefaultHeaders();
    }

    /**
     * GatewayResponse contains response status, response headers and body with payload resp. error messages.
     */
    public GatewayResponse() {
        this.envHandler = new Environment();
        this.statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        this.body = EMPTY_JSON;
        this.generateDefaultHeaders();
    }

    /**
     * GatewayResponse convenience constructor to set response status and body with payload direct.
     */
    public GatewayResponse(final String body, final int status) {
        this.envHandler = new Environment();
        this.statusCode = status;
        this.body = body;
        this.generateDefaultHeaders();
    }
    @JacocoGenerated
    public String getBody() {
        return body;
    }

    @JacocoGenerated
    public Map<String, String> getHeaders() {
        return headers;
    }

    @JacocoGenerated
    public int getStatusCode() {
        return statusCode;
    }

    @JacocoGenerated
    public void setBody(String body) {
        this.body = body;
    }

    @JacocoGenerated
    public void setStatusCode(int status) {
        this.statusCode = status;
    }

    /**
     * Set error message as a json string to body.
     *
     * @param message message from exception
     */
    public void setErrorBody(String message) {
        JsonObject json = new JsonObject();
        json.addProperty(ERROR_KEY, message);
        this.body = json.toString();
    }

    public void appendBody(String body) {
        this.body += body;
    }

    private void generateDefaultHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        String corsAllowDomain;
        try {
            corsAllowDomain = envHandler.readEnv(ALLOWED_ORIGIN_KEY);
        } catch (IllegalStateException e) {
            //We need to catch this but dont want to handle it
            corsAllowDomain = "";
        }
        if (StringUtils.isNotEmpty(corsAllowDomain)) {
            headers.put(CORS_ALLOW_ORIGIN_HEADER, corsAllowDomain);
        }
        headers.put("Access-Control-Allow-Methods", "OPTIONS,GET");
        headers.put("Access-Control-Allow-Credentials", "true");
        headers.put("Access-Control-Allow-Headers", HttpHeaders.CONTENT_TYPE);
        this.headers = Map.copyOf(headers);
    }
}
