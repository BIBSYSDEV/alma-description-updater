package no.unit.http;

import no.unit.alma.Config;
import nva.commons.core.JacocoGenerated;

public class AlmaConnectionFactory extends ConnectionFactory {

    private final Config config;
    private final HttpClientFactory httpClientFactory;

    @JacocoGenerated
    public AlmaConnectionFactory() {
        this(new Config(), new DefaultHttpClientFactory());
    }

    public AlmaConnectionFactory(Config config, HttpClientFactory httpClientFactory) {
        super();
        this.config = config;
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    protected Connection createConnection() {
        return new AlmaConnection(config, httpClientFactory);
    }

}
