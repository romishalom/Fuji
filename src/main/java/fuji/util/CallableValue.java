package fuji.util;

public interface CallableValue extends Value {
    boolean vararg();

    @Override
    SignatureValue type();
}
