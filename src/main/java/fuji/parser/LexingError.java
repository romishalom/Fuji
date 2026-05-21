package fuji.parser;

public class LexingError extends RuntimeException {
    public LexingError(String message) {
        super(message);
    }
}
