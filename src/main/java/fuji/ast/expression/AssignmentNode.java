package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.Visitor;

public record AssignmentNode(ExpressionNode target, ExpressionNode value) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
