package no.unit.http;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public abstract class ConnectionFactory {

    public Connection create() {
        return createConnection();
    }

    protected abstract Connection createConnection();

}
