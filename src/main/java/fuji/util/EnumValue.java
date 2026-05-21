package fuji.util;

import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;

public record EnumValue(List<String> names) implements TypeValue {
    @Override
    public boolean isAssignableFrom(TypeValue source) {
        if (!(source instanceof EnumValue(List<String> sourceNames))) return false;

        return new HashSet<>(names).containsAll(sourceNames);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", \n");
        for (String name: names) {
            sj.add("\t" + name);
        }

        return "enum {\n" + sj + "\n}";
    }
}
