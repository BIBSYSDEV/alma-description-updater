package no.unit.http;

public abstract class GetConnectionFactory {

    public GetConnection create() {
        return createGetConnection();
    }

    protected abstract GetConnection createGetConnection();

}
