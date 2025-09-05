package no.unit.http;

public abstract class ConnectionFactory {

    public Connection create() {
        return createConnection();
    }

    protected abstract Connection createConnection();

}
