package no.unit.alma;

public class SecretRetrieverException extends Exception {
    public SecretRetrieverException(String message, Exception e) {
        super(message, e);
    }
}
