package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.Visitor;

public record IfExpressionNode(ExpressionNode condition, ExpressionNode pass, ExpressionNode fail) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
