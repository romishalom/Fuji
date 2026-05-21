package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.Visitor;
import fuji.parser.TokenType;

public record UnaryOperationNode(TokenType operator, ExpressionNode operand) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
