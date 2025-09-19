package no.unit.http;

import java.net.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularHttpClientFactory extends HttpClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(SingularHttpClientFactory.class);

    private static final String CLIENT_SINGLETON_HASHCODE = "HttpClient Singleton Hashcode: {}";

    @Override
    protected HttpClient createHttpClient() {
        var httpClient = SingularHttpClientEnum.INSTANCE.getHttpClient();
        logger.info(CLIENT_SINGLETON_HASHCODE, httpClient.hashCode());

        return httpClient;
    }

}
