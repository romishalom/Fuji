package fuji.ast.expression;

import fuji.ast.ASTNode;
import fuji.ast.ExpressionNode;
import fuji.ast.ParameterNode;
import fuji.ast.Visitor;

import java.util.List;

public record InterfaceBlockNode(List<ExpressionNode> parents, List<ParameterNode> fields) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
