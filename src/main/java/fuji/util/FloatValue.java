package fuji.util;

public record FloatValue(double value) implements NumberValue {
    @Override
    public PrimitiveType type() {
        return PrimitiveType.FLOAT;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public Value add(Value other) {
        if (other instanceof IntValue(int otherValue)) {
            return new FloatValue(value + otherValue);
        } else if (other instanceof FloatValue(double otherValue)) {
            return new FloatValue(value + otherValue);
        }

        return NumberValue.super.add(other);
    }

    @Override
    public NumberValue subtract(Value other) {
        if (other instanceof IntValue(int otherValue)) {
            return new FloatValue(value - otherValue);
        } else if (other instanceof FloatValue(double otherValue)) {
            return new FloatValue(value - otherValue);
        }

        return NumberValue.super.subtract(other);
    }

    @Override
    public NumberValue multiply(Value other) {
        if (other instanceof IntValue(int otherValue)) {
            return new FloatValue(value * otherValue);
        } else if (other instanceof FloatValue(double otherValue)) {
            return new FloatValue(value * otherValue);
        }

        return NumberValue.super.multiply(other);
    }

    @Override
    public NumberValue divide(Value other) {
        if (other instanceof IntValue(int otherValue)) {
            return new FloatValue(value / otherValue);
        } else if (other instanceof FloatValue(double otherValue)) {
            return new FloatValue(value / otherValue);
        }

        return NumberValue.super.subtract(other);
    }

    @Override
    public NumberValue power(Value other) {
        if (other instanceof IntValue(int otherValue)) {
            return new FloatValue(Math.pow(value, otherValue));
        } else if (other instanceof FloatValue(double otherValue)) {
            return new FloatValue(Math.pow(value, otherValue));
        }

        return NumberValue.super.power(other);
    }

    @Override
    public NumberValue module(Value other) {
        if (other instanceof IntValue(int otherValue)) {
            return new FloatValue(value % otherValue);
        } else if (other instanceof FloatValue(double otherValue)) {
            return new FloatValue(value % otherValue);
        }

        return NumberValue.super.module(other);
    }

    @Override
    public BoolValue greaterThan(Value other) {
        if (other instanceof IntValue(int otherValue)) {
            return value > otherValue ? BoolValue.TRUE : BoolValue.FALSE;
        } else if (other instanceof FloatValue(double otherValue)) {
            return value > otherValue ? BoolValue.TRUE : BoolValue.FALSE;
        }

        return NumberValue.super.greaterThan(other);
    }

    @Override
    public BoolValue greaterThanOrEqualsTo(Value other) {
        if (other instanceof IntValue(int otherValue)) {
            return value >= otherValue ? BoolValue.TRUE : BoolValue.FALSE;
        } else if (other instanceof FloatValue(double otherValue)) {
            return value >= otherValue ? BoolValue.TRUE : BoolValue.FALSE;
        }

        return NumberValue.super.greaterThanOrEqualsTo(other);
    }

    @Override
    public BoolValue lessThan(Value other) {
        if (other instanceof IntValue(int otherValue)) {
            return value < otherValue ? BoolValue.TRUE : BoolValue.FALSE;
        } else if (other instanceof FloatValue(double otherValue)) {
            return value < otherValue ? BoolValue.TRUE : BoolValue.FALSE;
        }

        return NumberValue.super.lessThan(other);
    }

    @Override
    public BoolValue lessThanOrEqualsTo(Value other) {
        if (other instanceof IntValue(int otherValue)) {
            return value <= otherValue ? BoolValue.TRUE : BoolValue.FALSE;
        } else if (other instanceof FloatValue(double otherValue)) {
            return value <= otherValue ? BoolValue.TRUE : BoolValue.FALSE;
        }

        return NumberValue.super.lessThanOrEqualsTo(other);
    }

    @Override
    public Value as(Value other) {
        if (other == PrimitiveType.INT) return new IntValue( (int) value);

        return NumberValue.super.as(other);
    }

    @Override
    public FloatValue negative() {
        return new FloatValue( -value);
    }
}