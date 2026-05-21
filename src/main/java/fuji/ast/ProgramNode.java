package fuji.ast;

import java.util.List;

public record ProgramNode(List<StatementNode> statements) implements ASTNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
