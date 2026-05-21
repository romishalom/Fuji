package fuji.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public record ObjectValue(Map<String, Value> value) implements Value {
    public ObjectValue() {
        this(new HashMap<>());
    }

    @Override
    public StructValue type() {
        Map<String, TypeValue> types = new LinkedHashMap<>();
        value.forEach( (key, value) -> types.put(key, value.type()));

        return new StructValue(types);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", \n");
        value.forEach( (name, value) -> {
            sj.add("\t" + name + " = " + value);
        });

        return "object {\n" + sj + "\n}";
    }
}
