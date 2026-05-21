package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.StatementNode;
import fuji.ast.Visitor;

import java.util.List;

public record BlockNode(List<StatementNode> body) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
