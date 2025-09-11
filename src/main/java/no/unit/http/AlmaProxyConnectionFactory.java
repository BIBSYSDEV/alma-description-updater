package no.unit.http;

import no.unit.alma.Config;
import nva.commons.core.JacocoGenerated;

public class AlmaProxyConnectionFactory extends GetConnectionFactory {

    private final Config config;
    private final HttpClientFactory httpClientFactory;

    @JacocoGenerated
    public AlmaProxyConnectionFactory() {
        this(new Config(), new DefaultHttpClientFactory());
    }

    public AlmaProxyConnectionFactory(Config config, HttpClientFactory httpClientFactory) {
        super();
        this.config = config;
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    protected GetConnection createGetConnection() {
        return new AlmaProxyConnection(config, httpClientFactory);
    }

}
