package no.unit.alma;

public class ParsingException extends Exception {
    public ParsingException(String message, Exception e) {
        super(message, e);
    }
}
