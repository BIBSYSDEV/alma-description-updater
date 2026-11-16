package no.unit.exceptions;

public class HttpOperationFailedException extends RuntimeException {

    public HttpOperationFailedException(String message) {
        super(message);
    }

}
