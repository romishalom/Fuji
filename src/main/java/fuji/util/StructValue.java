package fuji.util;

import java.util.Map;
import java.util.StringJoiner;

public record StructValue(FunctionValue constructor, InterfaceTypeValue interfaceTypeValue,
                          Map<String, Value> value) implements TypeValue, PrimitiveValue {
    @Override
    public PrimitiveType type() {
        return PrimitiveType.TYPE;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", \n");
        value.forEach((name, type) -> {
            sj.add("\t" + name + ": " + type);
        });

        return "struct {\n" + sj + "\n}";
    }

    @Override
    public boolean isAssignableFrom(TypeValue source) {
        if (!(source instanceof StructValue(
                FunctionValue sourceConstructor,
                InterfaceTypeValue sourceInterfaceTypeValue,
                Map<String, Value> sourceValue
        )))
            return false;

        return interfaceTypeValue.isAssignableFrom(sourceInterfaceTypeValue)
                && value.entrySet().containsAll(sourceValue.entrySet())
                && constructor.equals(sourceConstructor);
    }
}