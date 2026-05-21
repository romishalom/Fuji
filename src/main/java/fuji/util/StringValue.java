package fuji.util;

public record StringValue(String value) implements PrimitiveValue {
    @Override
    public PrimitiveType type() {
        return PrimitiveType.STRING;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
