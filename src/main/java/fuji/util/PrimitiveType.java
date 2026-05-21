package fuji.util;

public enum PrimitiveType implements TypeValue {
    ANY,
    VOID,
    EMPTY,
    BOOL,
    INT,
    FLOAT,
    STRING,
    TYPE;

    @Override
    public boolean isAssignableFrom(TypeValue source) {
        return this == source || this == ANY || this == EMPTY;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
