package no.unit.http;

import no.unit.alma.Config;
import nva.commons.core.JacocoGenerated;

public class AlmaProxyConnectionFactory extends ReadConnectionFactory {

    private final Config config;
    private final HttpClientFactory httpClientFactory;

    @JacocoGenerated
    public AlmaProxyConnectionFactory() {
        this(new Config(), new SingularHttpClientFactory());
    }

    public AlmaProxyConnectionFactory(Config config, HttpClientFactory httpClientFactory) {
        super();
        this.config = config;
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    protected ReadConnection createConnection() {
        return new AlmaProxyConnection(config, httpClientFactory);
    }

}
