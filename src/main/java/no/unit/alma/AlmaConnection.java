package no.unit.alma;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import no.unit.http.HttpClientFactory;

public final class AlmaConnection {

    private static final String AUTHORIZATION = "Authorization";
    private static final String AUTHORIZATION_KEY = "apikey ";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_XML = "application/xml";

    private final HttpClient httpClient;
    private final Config config;

    public AlmaConnection(Config config, HttpClientFactory httpClientFactory) {
        this.config = config;
        this.httpClient = httpClientFactory.create();
    }

    /**
     * Sends a get request to the Alma api.
     * @param mmsId the mms_id of the bib-post you want to retrieve
     * @return the http-response in the shape of a String
     * @throws IOException When something goes wrong.
     * @throws InterruptedException When something goes wrong.
     */
    public HttpResponse<String> sendGet(String mmsId) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(URI.create(config.almaApiHost + mmsId))
                          .setHeader(AUTHORIZATION, AUTHORIZATION_KEY + config.secretKey)
                          .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends a put request to the Alma api.
     * @param mmsId the mms_id of the bib-post you want to update
     * @param xml the new xml that should replace the old bib-post
     * @return the Http-response in the form of a String
     * @throws IOException When something goes wrong.
     * @throws InterruptedException When something goes wrong.
     */
    public HttpResponse<String> sendPut(String mmsId, String xml) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                          .PUT(HttpRequest.BodyPublishers.ofString(xml))
                          .uri(URI.create(config.almaApiHost + mmsId))
                          .setHeader(AUTHORIZATION, AUTHORIZATION_KEY + config.secretKey)
                          .header(CONTENT_TYPE, APPLICATION_XML)
                          .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
