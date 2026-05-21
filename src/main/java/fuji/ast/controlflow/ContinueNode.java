package fuji.ast.controlflow;

import fuji.ast.StatementNode;
import fuji.ast.Visitor;

public record ContinueNode() implements StatementNode {
    public static final ContinueNode INSTANCE = new ContinueNode();

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
