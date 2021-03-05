package no.unit.alma;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AlmaConnection {

    private static final  String URL_STRING = System.getenv("ALMA_API_HOST");
    private static final  String AUTHORIZATION_KEY = "Authorization";
    private static final  String APIKEY_KEY = "apikey";
    private static final  String CONTENT_TYPE_KEY = "Content-Type";
    private static final  String XML_KEY = "application/xml";
    private static final  String SPACE_KEY = " ";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    /**
     * Sends a get request to the Alma api.
     * @param mmsId the mms_id of the bib-post you want to retrieve
     * @param apiKey the api_key needed to access the api
     * @return the http-response in the shape of a String
     * @throws IOException When something goes wrong.
     * @throws InterruptedException When something goes wrong.
     */
    public HttpResponse<String> sendGet(String mmsId, String apiKey)
            throws IOException,  InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(URL_STRING + mmsId))
                .setHeader(AUTHORIZATION_KEY, APIKEY_KEY + SPACE_KEY + apiKey)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response;
    }

    /**
     * Sends a put request to the Alma api.
     * @param mmsId the mms_id of the bib-post you want to update
     * @param apiKey the api_key needed to access the api
     * @param xml the new xml that should replace the old bib-post
     * @return the Http-response in the form of a String
     * @throws IOException When something goes wrong.
     * @throws InterruptedException When something goes wrong.
     */
    public HttpResponse<String> sendPut(String mmsId, String apiKey, String xml)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(xml))
                .uri(URI.create(URL_STRING + mmsId))
                .setHeader(AUTHORIZATION_KEY, APIKEY_KEY + SPACE_KEY + apiKey) // add request header
                .header(CONTENT_TYPE_KEY, XML_KEY)
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response;
    }

}
