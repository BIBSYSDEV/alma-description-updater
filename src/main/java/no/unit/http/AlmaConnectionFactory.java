package no.unit.http;

import no.unit.alma.Config;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class AlmaConnectionFactory extends ConnectionFactory {

    @Override
    protected Connection createConnection() {
        return new AlmaConnection(new Config(), new DefaultHttpClientFactory());
    }

}
