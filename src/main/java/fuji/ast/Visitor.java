package fuji.ast;

import com.romishalom.ast.controlflow.*;
import com.romishalom.ast.expression.*;
import fuji.ast.controlflow.*;
import fuji.ast.expression.*;

public interface Visitor<R> {
    R visit(ProgramNode node);
    R visit(ParameterNode node);
    R visit(PassNode node);
    R visit(PropertyNode node);

    R visit(DeclarationNode node);
    R visit(AssignmentNode node);
    R visit(ReturnStatementNode node);
    R visit(BreakNode node);
    R visit(ContinueNode node);
    R visit(IfStatementNode node);
    R visit(ExpressionStatementNode node);
    R visit(WhileLoopNode node);
    R visit(ForLoopNode node);

    R visit(BlockNode node);
    R visit(ObjectBlockNode node);
    R visit(AccessNode node);
    R visit(CallNode node);
    R visit(FunctionNode node);
    R visit(ReferenceNode node);
    R visit(ArrayNode node);
    R visit(LiteralNode node);
    R visit(UnaryOperationNode node);
    R visit(BinaryOperationNode node);
    R visit(IfExpressionNode node);
    R visit(IndexAccessNode node);
    R visit(StructBlockNode node);
    R visit(EnumBlockNode node);
    R visit(TupleNode node);
}
