package fuji.util;

import java.util.List;

public interface Value {
    ObjectValue DEFAULT_COMPANION_OBJECT = new ObjectValue();

    TypeValue type();

    default Value add(Value other) {
        if (other instanceof StringValue(String value)) {
            return new StringValue(this + value);
        }

        throw new UnsupportedOperationException("Addition operation ('+') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }
    default NumberValue subtract(Value other) {
        throw new UnsupportedOperationException("Subtraction operation ('-') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }
    default NumberValue multiply(Value other) {
        throw new UnsupportedOperationException("Multiplication operation ('*') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }
    default NumberValue divide(Value other) {
        throw new UnsupportedOperationException("Division operation ('/') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }
    default NumberValue power(Value other) {
        throw new UnsupportedOperationException("Exponentiation operation ('^') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }
    default NumberValue module(Value other) {
        throw new UnsupportedOperationException("Module operation ('%') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }

    default BoolValue and(Value other) {
        return isTruthy().and(other);
    }
    default BoolValue or(Value other) {
        return isTruthy().or(other);
    }
    default BoolValue is(Value other) {
        if (other instanceof TypeValue typeValue) {
            return type().isAssignableFrom(typeValue) ? BoolValue.TRUE : BoolValue.FALSE;
        }

        throw new UnsupportedOperationException("Is operation ('is') not supported for types '" + type() + "' and " + other.type() + "'.");
    }

    default BoolValue equalsTo(Value other) {
        return equals(other) ? BoolValue.TRUE : BoolValue.FALSE;
    }
    default BoolValue notEqualsTo(Value other) {
        return equalsTo(other).not();
    }
    default BoolValue greaterThan(Value other) {
        throw new UnsupportedOperationException("Comparison operation ('>') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }
    default BoolValue greaterThanOrEqualsTo(Value other) {
        throw new UnsupportedOperationException("Comparison operation ('>=') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }
    default BoolValue lessThan(Value other) {
        throw new UnsupportedOperationException("Comparison operation ('<') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }
    default BoolValue lessThanOrEqualsTo(Value other) {
        throw new UnsupportedOperationException("Comparison operation ('<=') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }

    default TypeUnionValue union(Value other) {
        throw new UnsupportedOperationException("Type union operation ('|') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }
    default TypeIntersectionValue intersection(Value other) {
        throw new UnsupportedOperationException("Type intersection operation ('&') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }

    default Value as(Value other) {
        if (other instanceof TypeValue typeValue) {
            if (type().isAssignableFrom(typeValue) || typeValue == PrimitiveType.ANY) {
                return this;
            } else if (typeValue == PrimitiveType.STRING) {
                return new StringValue(toString());
            } else if(typeValue == PrimitiveType.BOOL) {
                return isTruthy();
            }
        }

        throw new UnsupportedOperationException("As operation ('as') not supported for types '" + type() + "' and '" + other.type() + "'.");
    }

    default BoolValue isTruthy() {
        return BoolValue.TRUE;
    }
    default BoolValue not() {
        return isTruthy().not();
    }
    default NumberValue negative() {
        throw new UnsupportedOperationException("Negation operation ('-') not supported for type '" + type() + "'.");
    }

    default Value access(String name) {
        Value value = companionObject().get(new StringValue(name));
        if (value == null) throw new UnsupportedOperationException("Access operation ('.') not supported for type '" + type() + "' with name '" + name + "'.");

        return value;
    }

    default Value get(Value key) {
        throw new UnsupportedOperationException("Get operation ('<iterable>[<key>]') not supported for type '" + type() + "' with key type '" + key.type() + "'.");
    }

    default Value set(Value index, Value value) {
        throw new UnsupportedOperationException("Set operation ('<iterable>[<key>] = <value>') not supported for type '" + type() + "' with key type '" + index.type() + "'.");
    }

    default Value call(List<Value> args) {
        throw new UnsupportedOperationException("Call operation ('callee(args...)') is not supported for type '" + type() + "'.");
    }

    default ObjectValue companionObject() {
        return DEFAULT_COMPANION_OBJECT;
    }
}
