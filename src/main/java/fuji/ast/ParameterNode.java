package fuji.ast;

public record ParameterNode(String name, ExpressionNode type) implements ASTNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
