package fuji.ast.controlflow;

import fuji.ast.StatementNode;
import fuji.ast.Visitor;

public record BreakNode() implements StatementNode {
    public static final BreakNode INSTANCE = new BreakNode();

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}