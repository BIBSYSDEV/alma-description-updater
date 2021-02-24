package no.unit.alma;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AlmaConnection {

    private final static String URL_STRING = "https://api-eu.hosted.exlibrisgroup.com/almaws/v1/bibs/";
    private final static String AUTHORIZATION_KEY = "Authorization";
    private final static String APIKEY_KEY = "apikey";
    private final static String CONTENT_TYPE_KEY = "Content-Type";
    private final static String XML_KEY = "application/xml";
    private final static String SPACE_KEY = " ";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    public HttpResponse<String> sendGet(String mms_id, String api_key) throws IOException, IllegalArgumentException, InterruptedException, SecurityException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(URL_STRING + mms_id))
                .setHeader(AUTHORIZATION_KEY, APIKEY_KEY + SPACE_KEY + api_key)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response;
    }

    public HttpResponse<String> sendPut(String mms_id, String api_key, String xml) throws IOException, IllegalArgumentException, InterruptedException, SecurityException{
        HttpRequest request = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(xml))
                .uri(URI.create(URL_STRING + mms_id))
                .setHeader(AUTHORIZATION_KEY, APIKEY_KEY + SPACE_KEY + api_key) // add request header
                .header(CONTENT_TYPE_KEY, XML_KEY)
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response;
    }

}
