package fuji.util;

public record TypeIntersectionValue(TypeValue left, TypeValue right) implements TypeValue {
    @Override
    public boolean isAssignableFrom(TypeValue source) {
        return left.isAssignableFrom(source) && right.isAssignableFrom(source);
    }

    @Override
    public String toString() {
        return "(" + left + ") & (" + right + ")";
    }
}
