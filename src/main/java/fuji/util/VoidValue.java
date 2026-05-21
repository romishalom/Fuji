package fuji.util;

public enum VoidValue implements PrimitiveValue<Object> {
    UNIT;

    @Override
    public String toString() {
        return "unit";
    }

    @Override
    public PrimitiveType type() {
        return PrimitiveType.VOID;
    }

    @Override
    public Object value() {
        return null;
    }
}
