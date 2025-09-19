package no.unit.http;

import java.net.http.HttpClient;

public enum SingularHttpClientEnum {

    INSTANCE;

    private final HttpClient httpClient = createHttpClient();

    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                   .version(HttpClient.Version.HTTP_2)
                   .build();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

}
