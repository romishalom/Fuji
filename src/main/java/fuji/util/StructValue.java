package fuji.util;

import java.util.Map;
import java.util.StringJoiner;

public record StructValue(, Map<String, TypeValue> value) implements TypeValue, PrimitiveValue {
    public static StructValue OBJECT = new StructValue(Map.of());

    @Override
    public PrimitiveType type() {
        return PrimitiveType.TYPE;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", \n");
        value.forEach( (name, type) -> {
            sj.add("\t" + name + ": " + type);
        });

        return "struct {\n" + sj + "\n}";
    }

    @Override
    public boolean isAssignableFrom(TypeValue source) {
        if (!(source instanceof StructValue(Map<String, TypeValue> sourceValue)))
            return false;

        return sourceValue.entrySet().containsAll(value.entrySet());
    }
}