package fuji.ast;

public record PassNode() implements StatementNode {
    public static final PassNode INSTANCE = new PassNode();

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
