package no.unit.http;

public abstract class ReadUpdateConnectionFactory {

    public ReadUpdateConnection create() {
        return createConnection();
    }

    protected abstract ReadUpdateConnection createConnection();

}
