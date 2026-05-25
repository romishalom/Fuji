package fuji.util;

import java.util.List;
import java.util.StringJoiner;

public record SignatureValue(List<TypeValue> parameterTypes, TypeValue returnType) implements TypeValue {
    @Override
    public boolean isAssignableFrom(TypeValue source) {
        if (!(source instanceof SignatureValue(List<TypeValue> sourceParameterTypes, TypeValue sourceReturnType)))
            return false;

        return parameterTypes.equals(sourceParameterTypes) && returnType.isAssignableFrom(sourceReturnType);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(",");
        for (TypeValue parameterType : parameterTypes) {
            sj.add(parameterType.toString());
        }

        return "Function(" + sj + ")(" + returnType + ")";
    }
}
