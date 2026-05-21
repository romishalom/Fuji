package fuji.util;

public record MapTypeValue(TypeValue keyType, TypeValue valueType) implements TypeValue {
    @Override
    public boolean isAssignableFrom(TypeValue source) {
        if (source instanceof MapTypeValue(TypeValue otherKeyType, TypeValue otherValueType)) {
            return keyType.isAssignableFrom(otherKeyType) && valueType.isAssignableFrom(otherValueType);
        }

        return false;
    }
}
