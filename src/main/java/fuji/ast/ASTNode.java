package fuji.ast;

public interface ASTNode {
    <R> R accept(Visitor<R> visitor);
}
