package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.ParameterNode;
import fuji.ast.Visitor;

import java.util.List;

public record FunctionNode(List<ParameterNode> parameters, boolean vararg, ExpressionNode returnType, ExpressionNode body) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
