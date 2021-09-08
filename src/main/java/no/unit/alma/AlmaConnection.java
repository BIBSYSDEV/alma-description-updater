package no.unit.alma;

import nva.commons.core.JacocoGenerated;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class AlmaConnection {

    private static AlmaConnection instance = new AlmaConnection();

    private static final  String AUTHORIZATION_KEY = "Authorization";
    private static final  String APIKEY_KEY = "apikey";
    private static final  String SPACE_KEY = " ";
    private static final Config config = new Config();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    private AlmaConnection(){
    }

    @JacocoGenerated
    public static AlmaConnection getInstance() {
        return instance;
    }

    /**
     * Sends a get request to the Alma api.
     * @param mmsId the mms_id of the bib-post you want to retrieve
     * @return the http-response in the shape of a String
     * @throws IOException When something goes wrong.
     * @throws InterruptedException When something goes wrong.
     */
    @JacocoGenerated
    public HttpResponse<String> sendGet(String mmsId)
            throws IOException,  InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(config.almaApiHost + mmsId))
                .setHeader(AUTHORIZATION_KEY, APIKEY_KEY + SPACE_KEY + config.secretKey)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response;
    }

    /**
     * Sends a put request to the Alma api.
     * @param mmsId the mms_id of the bib-post you want to update
     * @param xml the new xml that should replace the old bib-post
     * @return the Http-response in the form of a String
     * @throws IOException When something goes wrong.
     * @throws InterruptedException When something goes wrong.
     */
    @JacocoGenerated
    public HttpResponse<String> sendPut(String mmsId, String xml)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(xml))
                .uri(URI.create(config.almaApiHost + mmsId))
                .setHeader(AUTHORIZATION_KEY, APIKEY_KEY + SPACE_KEY + config.secretKey) // add request header
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response;
    }

}
