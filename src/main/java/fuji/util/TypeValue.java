package fuji.util;

public interface TypeValue extends Value {
    boolean isAssignableFrom(TypeValue source);

    @Override
    default TypeValue type() {
        return PrimitiveType.TYPE;
    }

    @Override
    default TypeUnionValue union(Value other) {
        if (other instanceof TypeValue otherTypeValue) {
            return new TypeUnionValue(this, otherTypeValue);
        }

        return Value.super.union(other);
    }

    @Override
    default TypeIntersectionValue intersection(Value other) {
        if (other instanceof TypeValue otherTypeValue) {
            return new TypeIntersectionValue(this, otherTypeValue);
        }

        return Value.super.intersection(other);
    }
}
