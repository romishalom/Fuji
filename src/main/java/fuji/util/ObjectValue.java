package fuji.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public record ObjectValue(InterfaceTypeValue type, Map<String, Value> value) implements Value {
    public ObjectValue() {
        this(new InterfaceTypeValue(), new HashMap<>());
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
