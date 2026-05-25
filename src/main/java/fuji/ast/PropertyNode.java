package fuji.ast;

public record PropertyNode(String name, ExpressionNode type, ExpressionNode value) implements ASTNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
