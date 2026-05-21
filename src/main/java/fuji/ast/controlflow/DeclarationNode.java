package fuji.ast.controlflow;

import fuji.ast.ExpressionNode;
import fuji.ast.StatementNode;
import fuji.ast.Visitor;

public record DeclarationNode(boolean isConstant, ExpressionNode target, ExpressionNode type, ExpressionNode initializer) implements StatementNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
