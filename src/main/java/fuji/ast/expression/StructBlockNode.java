package fuji.ast.expression;

import fuji.ast.*;

import java.util.List;

public record StructBlockNode(List<ParameterNode> constructorParameters, List<ExpressionNode> parents, ExpressionNode constructorBody) implements ExpressionNode {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
