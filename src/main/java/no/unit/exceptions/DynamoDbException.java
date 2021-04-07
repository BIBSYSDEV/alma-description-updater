package no.unit.exceptions;

public class DynamoDbException extends Exception {
    public DynamoDbException(String message, Exception e) {
        super(message, e);
    }
}
