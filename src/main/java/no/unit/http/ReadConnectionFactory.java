package no.unit.http;

public abstract class ReadConnectionFactory {

    public ReadConnection create() {
        return createConnection();
    }

    protected abstract ReadConnection createConnection();

}
