package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.Visitor;
import fuji.util.Value;

public record LiteralNode(Value value) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
