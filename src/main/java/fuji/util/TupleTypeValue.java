package fuji.util;

import java.util.List;
import java.util.StringJoiner;

public record TupleTypeValue(List<TypeValue> types) implements TypeValue {
    @Override
    public boolean isAssignableFrom(TypeValue source) {
        if (!(source instanceof TupleTypeValue(List<TypeValue> sourceTypes)))
            return false;

        return sourceTypes.equals(types);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (TypeValue type: types) {
            sj.add(type.toString());
        }

        return "tuple(" + sj + ")";
    }
}
