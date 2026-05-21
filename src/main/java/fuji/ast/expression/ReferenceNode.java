package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.Visitor;

public record ReferenceNode(String name) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
