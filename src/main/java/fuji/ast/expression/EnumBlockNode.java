package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.Visitor;

import java.util.List;

public record EnumBlockNode(List<String> names) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
