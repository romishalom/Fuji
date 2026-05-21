package fuji.util;

import java.util.List;
import java.util.function.Function;

public record FunctionValue(boolean vararg, SignatureValue type, Function<List<Value>, Value> function) implements CallableValue {
    @Override
    public String toString() {
        return "<" + type + ">";
    }

    @Override
    public Value call(List<Value> args) {
        // Verify argument types matches signature
        for (int i = 0; i < (vararg ? type.parameterTypes().size() - 1 : args.size()); i++) {
            Value arg = args.get(i);
            TypeValue parameterType = type.parameterTypes().get(i);
            if (!parameterType.isAssignableFrom(arg.type()))
                throw new RuntimeException("Cannot execute call operation. Argument type '" + arg.type() + "' is not assignable to parameter type '" + parameterType + "'.");
        }

        if (vararg) {
            TypeValue varargType = type.parameterTypes().getLast();
            for (int i = type.parameterTypes().size() - 1; i < args.size(); i++) {
                Value arg = args.get(i);
                if (!varargType.isAssignableFrom(arg.type()))
                    throw new RuntimeException("Cannot execute call operation. Argument type '" + arg.type() + "' is not assignable to parameter type '" + varargType + "'.");
            }
        }

        Value returnedValue = function.apply(args);

        if (!type.returnType().isAssignableFrom(returnedValue.type()))
            throw new RuntimeException("Cannot execute call operation. Type '" + returnedValue.type() + "' is not assignable to type '" + type.returnType() + "'.");


        return returnedValue;
    }
}
