package fuji.util;

public enum BoolValue implements PrimitiveValue {
    TRUE(true), FALSE(false);

    private final boolean value;
    BoolValue(boolean value) {
        this.value = value;
    }

    public boolean value() {
        return value;
    }

    @Override
    public PrimitiveType type() {
        return PrimitiveType.BOOL;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public BoolValue isTruthy() {
        return this;
    }

    @Override
    public BoolValue not() {
        return value ? FALSE: TRUE;
    }

    @Override
    public BoolValue and(Value other) {
        return value && other.isTruthy().value? TRUE: FALSE;
    }

    @Override
    public BoolValue or(Value other) {
        return value || other.isTruthy().value? TRUE: FALSE;
    }

    @Override
    public Value as(Value other) {
        if (other == PrimitiveType.INT) return new IntValue(value ? 1 : 0);
        else if (other == PrimitiveType.FLOAT) return new FloatValue(value ? 1.0 : 0.0);

        return PrimitiveValue.super.as(other);
    }
}
