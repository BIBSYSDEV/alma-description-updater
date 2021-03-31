package no.unit.exceptions;

public class SecretRetrieverException extends Exception {
    public SecretRetrieverException(String message, Exception e) {
        super(message, e);
    }
}
