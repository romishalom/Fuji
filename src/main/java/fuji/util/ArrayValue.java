package fuji.util;

import java.util.List;

public record ArrayValue(ArrayTypeValue type, List<Value> values) implements Value {
    private static final ObjectValue companionObject = new ObjectValue();
    static {
        // Initialize the access block

    }

    @Override
    public String toString() {
        return String.valueOf(values);
    }

    @Override
    public Value get(Value key) {
        if (key instanceof IntValue(int index)) {
            return values.get(index);
        }

        return Value.super.get(key);
    }

    @Override
    public Value set(Value index, Value value) {

    }
}
