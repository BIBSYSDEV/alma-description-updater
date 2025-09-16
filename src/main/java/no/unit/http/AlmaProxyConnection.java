package no.unit.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import no.unit.alma.Config;
import nva.commons.core.JacocoGenerated;

public class AlmaProxyConnection implements ReadConnection {

    private final HttpClient httpClient;
    private final Config config;

    @SuppressWarnings("unused")
    @JacocoGenerated
    public AlmaProxyConnection() {
        this(new Config(), new DefaultHttpClientFactory());
    }

    public AlmaProxyConnection(Config config, HttpClientFactory httpClientFactory) {
        this.config = config;
        this.httpClient = httpClientFactory.create();
    }

    @Override
    public HttpResponse<String> sendGet(String isbn) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(URI.create(config.getAlmaSruHost() + isbn))
                          .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
