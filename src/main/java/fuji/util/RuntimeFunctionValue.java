package fuji.util;

import fuji.ast.ExpressionNode;
import fuji.runtime.Scope;

import java.util.List;

public record RuntimeFunctionValue(List<String> parameterNames, boolean vararg, SignatureValue type, ExpressionNode body, Scope closure) implements CallableValue {
    @Override
    public String toString() {
        return "<lambda " + type + ">";
    }
}
