package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.PropertyNode;
import fuji.ast.Visitor;

import java.util.List;

public record ObjectBlockNode(List<ExpressionNode> parents, List<PropertyNode> body) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
