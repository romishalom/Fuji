package fuji.util;

import java.util.Map;

public record MapValue(MapTypeValue type, Map<Value, Value> values) implements Value {
    @Override
    public Value get(Value key) {
        return Value.super.get(key);
    }
}
