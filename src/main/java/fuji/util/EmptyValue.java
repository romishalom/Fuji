package fuji.util;

public enum EmptyValue implements PrimitiveValue {
    NONE;

    @Override
    public PrimitiveType type() {
        return PrimitiveType.EMPTY;
    }

    @Override
    public String toString() {
        return "none";
    }

    @Override
    public BoolValue isTruthy() {
        return BoolValue.FALSE;
    }
}
