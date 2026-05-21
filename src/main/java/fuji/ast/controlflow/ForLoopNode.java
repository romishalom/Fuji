package fuji.ast.controlflow;

import fuji.ast.ExpressionNode;
import fuji.ast.StatementNode;
import fuji.ast.Visitor;

public record ForLoopNode(String iterationSymbolName, ExpressionNode iterable, StatementNode body) implements StatementNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
