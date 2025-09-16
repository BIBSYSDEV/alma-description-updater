package no.unit.http;

import java.net.http.HttpClient;

public class DefaultHttpClientFactory extends HttpClientFactory {

    @Override
    protected HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                   .version(HttpClient.Version.HTTP_2)
                   .build();
    }

}
