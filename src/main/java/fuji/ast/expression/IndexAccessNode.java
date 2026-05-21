package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.Visitor;

public record IndexAccessNode(ExpressionNode list, ExpressionNode index) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
