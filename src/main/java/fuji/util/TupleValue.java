package fuji.util;

import java.util.List;
import java.util.StringJoiner;

public record TupleValue(TupleTypeValue type, List<Value> values) implements Value {
    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (Value value: values) {
            sj.add(value.toString());
        }

        return "<" + sj + ">";
    }

    @Override
    public Value get(Value key) {
        if (key instanceof IntValue(int index)) {
            return values.get(index);
        }

        return Value.super.get(key);
    }
}
