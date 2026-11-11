package no.unit.http;

import java.net.http.HttpClient;

public abstract class HttpClientFactory {

    public HttpClient create() {
        return createHttpClient();
    }

    protected abstract HttpClient createHttpClient();

}
