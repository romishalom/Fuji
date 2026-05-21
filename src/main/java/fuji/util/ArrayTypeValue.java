package fuji.util;

public record ArrayTypeValue(TypeValue value) implements TypeValue {
    @Override
    public boolean isAssignableFrom(TypeValue source) {
        if (!(source instanceof ArrayTypeValue(TypeValue sourceValue)))
            return false;

        return value.isAssignableFrom(sourceValue);
    }

    @Override
    public String toString() {
        return "Array(" + value + ")";
    }
}
