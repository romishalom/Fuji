package fuji.util;

import java.util.HashMap;
import java.util.Map;

public record InterfaceTypeValue(Map<String, TypeValue> value) implements TypeValue {
    public InterfaceTypeValue() {
        this(new HashMap<>());
    }

    @Override
    public boolean isAssignableFrom(TypeValue source) {
        if (source instanceof InterfaceTypeValue(Map<String, TypeValue> otherValue)) {
            return value.entrySet().containsAll(otherValue.entrySet());
        } else if (source instanceof StructValue())

        return false;
    }

    @Override
    public String toString() {
        return "interface {}"; //TODO
    }
}
