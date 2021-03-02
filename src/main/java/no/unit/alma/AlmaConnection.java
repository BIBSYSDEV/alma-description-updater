package no.unit.alma;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AlmaConnection {

    private final static String URL_STRING = System.getenv("ALMA_API_HOST");
    private final static String AUTHORIZATION_KEY = "Authorization";
    private final static String APIKEY_KEY = "apikey";
    private final static String CONTENT_TYPE_KEY = "Content-Type";
    private final static String XML_KEY = "application/xml";
    private final static String SPACE_KEY = " ";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    /**
     *
     * @param mms_id the mms_id of the bib-post you want to retrieve
     * @param api_key the api_key needed to access the api
     * @return the http-response in the shape of a String
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SecurityException
     */
    public HttpResponse<String> sendGet(String mms_id, String api_key) throws IOException, IllegalArgumentException, InterruptedException, SecurityException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(URL_STRING + mms_id))
                .setHeader(AUTHORIZATION_KEY, APIKEY_KEY + SPACE_KEY + api_key)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response;
    }

    /**
     *
     * @param mms_id the mms_id of the bib-post you want to update
     * @param api_key the api_key needed to access the api
     * @param xml the new xml that should replace the old bib-post
     * @return the Http-response in the form of a String
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SecurityException
     */
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
