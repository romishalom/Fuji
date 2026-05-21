package fuji.ast.expression;

import fuji.ast.ExpressionNode;
import fuji.ast.ParameterNode;

import java.util.List;

public record InterfaceBlockNode(List<ExpressionNode> parents, List<ParameterNode> fields) {
}
