package fuji.runtime;

import fuji.ast.*;
import fuji.ast.controlflow.*;
import fuji.ast.expression.*;
import fuji.util.*;

import java.util.*;
import java.util.function.Function;

public class Interpreter implements Visitor<Value> {

    private Scope currentScope = new Scope();

    private TypeValue evaluateToType(ExpressionNode expression) {
        if (expression == null) return null;
        Value evaluated = expression.accept(this);
        if (evaluated instanceof TypeValue typeValue)
            return typeValue;

        throw new RuntimeException("Cannot evaluate type expression. The expression does not evaluate to a type instance.");
    }

    public void interpret(ProgramNode node) {
        node.accept(this);
    }

    @Override
    public Value visit(ProgramNode node) {
        for (StatementNode statement: node.statements()) {
            statement.accept(this);
        }

        return VoidValue.UNIT;
    }

    @Override
    public Value visit(ParameterNode node) {
        return null;
    }

    @Override
    public Value visit(PassNode node) {
        return VoidValue.UNIT;
    }

    @Override
    public Value visit(DeclarationNode node) {
        Value evaluatedValue = null;
        TypeValue type = evaluateToType(node.type());

        if (node.initializer() != null) {
            evaluatedValue = node.initializer().accept(this);
            if (type == null)
                type = evaluatedValue.type();
            else if (!type.isAssignableFrom(evaluatedValue.type()))
                throw new RuntimeException("Cannot execute declaration. Type '" + evaluatedValue.type() + "' is not assignable to type '" + type + "'.");
        }

        if (node.target() instanceof ReferenceNode(String name)) {
            if (currentScope.hasLocal(name))
                throw new RuntimeException("Cannot execute declaration. Symbol '" + name + "' already exists");

            currentScope.declare(new Symbol(node.isConstant(), name, type, evaluatedValue));
        } else if (node.target() instanceof AccessNode(ExpressionNode parent, String name)) {


            Value evaluatedParent = parent.accept(this);

            if (!(evaluatedParent instanceof ObjectValue(Map<String, Value> binding))) throw new RuntimeException("Cannot execute declaration. External field declaration parent should be an object.");
            if (binding.containsKey(name)) throw new RuntimeException("Cannot execute declaration. Extension field '" + name + "' already exists");

            binding.put(name, evaluatedValue);
        } else {
            throw new RuntimeException("Cannot execute declaration. Declaration is not defined for the provided target expression.");
        }

        return VoidValue.UNIT;
    }

    @Override
    public Value visit(AssignmentNode node) {
        Value evaluatedValue = node.value().accept(this);
        switch (node.target()) {
            case ReferenceNode(String name) -> {
                Symbol symbol = currentScope.resolve(name);
                if (symbol == null)
                    throw new RuntimeException("Cannot execute assignment operation. Symbol '" + name + "' not found in the current scope.");
                else if (symbol.isConstant() && symbol.getValue() != null)
                    throw new RuntimeException("Cannot execute assignment operation. Symbol '" + name + "' is a constant.");

                symbol.setValue(evaluatedValue);
            }

            case AccessNode(ExpressionNode parent, String name) -> {
                Value evaluatedParent = parent.accept(this);
                if (evaluatedParent instanceof ObjectValue(Map<String, Value> map)) {
                    if (!map.containsKey(name))
                        throw new RuntimeException("Cannot execute assignment operation. Symbol '" + name + "' not found in the parent scope.");

                    map.put(name, evaluatedParent);
                }
            }

            case IndexAccessNode(ExpressionNode iterable, ExpressionNode index) -> {
                Value evaluatedList = iterable.accept(this);
                if (evaluatedList instanceof ArrayValue(ArrayTypeValue listType, List<Value> list)) {
                    Value evaluatedIndex = index.accept(this);
                    if (evaluatedIndex instanceof NumberValue<?> indexNumber) {
                        if (listType.value().isAssignableFrom(evaluatedValue.type())) {
                            list.set(indexNumber.value().intValue(), evaluatedValue);
                        } else throw new RuntimeException("Cannot execute assignment operation. Item type '" + evaluatedValue.type() + "' is not assignable to list type '" + listType + "'.");
                    } else
                        throw new UnsupportedOperationException("Cannot execute assignment operation. Index access operation is not defined for the provided index expression type.");
                } else if (evaluatedList instanceof TupleValue(TupleTypeValue tupleType, List<Value> values)) {
                    Value evaluatedIndex = index.accept(this);
                    if (evaluatedIndex instanceof NumberValue<?> indexNumber) {
                        int indexInt = indexNumber.value().intValue();
                        if (tupleType.types().get(indexInt).isAssignableFrom(evaluatedValue.type())) {
                            values.set(indexNumber.value().intValue(), evaluatedValue);
                        } else throw new RuntimeException("Cannot execute assignment operation. Item type '" + evaluatedValue.type() + "' is not assignable to tuple type '" + tupleType + "'.");
                    } else
                        throw new UnsupportedOperationException("Cannot execute assignment operation. Index access operation is not defined for the provided index expression type.");
                } else
                    throw new UnsupportedOperationException("Cannot execute assignment operation. Index access operation is not defined for the provided target expression type.");
            }

            default ->
                    throw new UnsupportedOperationException("Cannot execute assignment operation. Assignment operation is not defined for the provided target expression type.");
        }

        return evaluatedValue;
    }

    @Override
    public Value visit(ReturnStatementNode node) {
        throw new ReturnSignal(node.returnedValue().accept(this));
    }

    @Override
    public Value visit(BreakNode node) {
        throw BreakSignal.INSTANCE;
    }

    @Override
    public Value visit(ContinueNode node) {
        throw ContinueSignal.INSTANCE;
    }

    @Override
    public Value visit(IfStatementNode node) {
        Value evaluatedCondition = node.condition().accept(this);

        if (evaluatedCondition.isTruthy().value()) {
            node.pass().accept(this);
        } else {
            node.fail().accept(this);
        }

        return VoidValue.UNIT;
    }

    @Override
    public Value visit(ExpressionStatementNode node) {
        node.expression().accept(this);

        return VoidValue.UNIT;
    }

    @Override
    public Value visit(WhileLoopNode node) {
        Value evaluatedCondition = node.isPostCondition() ? BoolValue.TRUE : node.condition().accept(this);

        while (evaluatedCondition.isTruthy().value()) {
            try {
                node.body().accept(this);
            } catch (BreakSignal signal) {
                break;
            } catch (ContinueSignal ignored) {}

            evaluatedCondition = node.condition().accept(this);
        }

        return VoidValue.UNIT;
    }

    @Override
    public Value visit(ForLoopNode node) {
        Value iterable = node.iterable().accept(this);
        if (!(iterable instanceof ArrayValue(ArrayTypeValue arrayTypeValue, List<Value> list))) throw new RuntimeException("Cannot execute for loop. For loop iterable expression type must be a list.");

        Scope oldCurrentScope = currentScope;
        currentScope = new Scope(currentScope);

        for (Value value: list) {
            currentScope.declare(new Symbol( false, node.iterationSymbolName(), value.type(), value));

            try {
                node.body().accept(this);
            } catch (BreakSignal signal) {
                break;
            } catch (ContinueSignal ignored) {}
        }

        currentScope = oldCurrentScope;

        return VoidValue.UNIT;
    }

    @Override
    public Value visit(BlockNode node) {
        for (StatementNode statement: node.body()) {
            try {
                statement.accept(this);
            } catch (ReturnSignal returnSignal) {
                return returnSignal.getReturnedValue();
            }
        }

        return VoidValue.UNIT;
    }

    @Override
    public Value visit(ObjectBlockNode node) {
        Map<String, Value> map = new LinkedHashMap<>();
        for (ExpressionNode parent: node.parents()) {
            Value evaluatedParent = parent.accept(this);
            if (!(evaluatedParent instanceof ObjectValue(, Map<String, Value> objectMap))) throw new RuntimeException("Cannot execute object block. Parent expression type must be an object.");
            map.putAll(objectMap);
        }

        ObjectValue objectValue = new ObjectValue(map);

        Scope oldScope = currentScope;
        currentScope = new Scope(currentScope);

        // Define 'this'
        currentScope.declare(new Symbol(true, "this", objectValue.type(), objectValue));

        currentScope = oldScope;

        return new ObjectValue(map);
    }

    @Override
    public Value visit(AccessNode node) {
        Value evaluatedParent = node.parent().accept(this);

        if (evaluatedParent instanceof ObjectValue(Map<String, Value> map)) {
            if (map.containsKey(node.name()))
                return map.get(node.name());

            throw new RuntimeException("Cannot evaluate access operation. Symbol '" + node.name() + "' not found in the parent scope.");
        } else if (evaluatedParent instanceof EnumValue enumValue) {
            if (enumValue.names().contains(node.name()))
                return new EnumInstanceValue(node.name(), enumValue);

            throw new RuntimeException("Cannot evaluate access operation. Symbol '" + node.name() + "' not found in the parent scope.");
        }

        throw new UnsupportedOperationException("Cannot evaluate access operation. Operation 'access' is not defined for parent type '" + evaluatedParent.type() + "'.");
    }

    @Override
    public Value visit(CallNode node) {
        Value evaluatedCallee = node.callee().accept(this);
        List<Value> evaluatedArgs = new ArrayList<>();
        for (ExpressionNode argument: node.arguments()) {
            evaluatedArgs.add(argument.accept(this));
        }

        return evaluatedCallee.call(evaluatedArgs);
    }

    @Override
    public Value visit(FunctionNode node) {
        List<String> parameterNames = new ArrayList<>();
        List<TypeValue> parameterTypes = new ArrayList<>();
        boolean vararg = node.vararg();

        for (ParameterNode parameter: node.parameters()) {
            parameterNames.add(parameter.name());
            parameterTypes.add(evaluateToType(parameter.type()));
        }

        SignatureValue signatureValue = new SignatureValue(parameterTypes, evaluateToType(node.returnType()));

        Function<List<Value>, Value> function = (args) -> {
            Scope oldScope = currentScope;
            currentScope = new Scope(oldScope);

            for (int i = 0; i < (vararg ? parameterNames.size()-1 : parameterNames.size()); i++) {
                ParameterNode parameter = node.parameters().get(i);
                String parameterName = parameter.name();
                TypeValue parameterType = evaluateToType(parameter.type());

                currentScope.declare(new Symbol(false, parameterName, parameterType, args.get(i)));
            }

            if (vararg) {
                String varargName = parameterNames.getLast();
                TypeValue varargType = parameterTypes.getLast();
                ArrayTypeValue varargListType = new ArrayTypeValue(varargType);

                List<Value> varargValues = new ArrayList<>();
                for (int i = parameterNames.size()-1; i < args.size(); i++) {
                    Value arg = args.get(i);
                    if (!(varargType.isAssignableFrom(arg.type()))) throw new RuntimeException("Cannot execute call operation. Argument type '" + arg.type() + "' is not assignable to parameter type '" + varargType + "'.");
                    varargValues.add(arg);
                }

                ArrayValue varargValue = new ArrayValue(varargListType, varargValues);
                currentScope.declare(new Symbol(false, varargName, varargType, varargValue));
            }

            Value returnedValue = node.body().accept(this);

            currentScope = oldScope;

            return returnedValue;
        };

        return new FunctionValue(vararg, signatureValue, function);
    }

    @Override
    public Value visit(ReferenceNode node) {
        Symbol symbol = currentScope.resolve(node.name());

        if (symbol == null) {
            if (RuntimeOps.isBuiltin(node.name())) {
                return RuntimeOps.getBuiltin(node.name());
            } else throw new RuntimeException("Cannot evaluate reference expression. Symbol '" + node.name() + "' not found in the current scope.");
        }

        return symbol.getValue();
    }

    @Override
    public Value visit(ArrayNode node) {
        List<Value> list = new ArrayList<>();
        ArrayTypeValue listType = null;

        for (ExpressionNode item: node.items()) {
            Value evaluatedItem = item.accept(this);

            if (listType == null) {
                listType = new ArrayTypeValue(evaluatedItem.type());
            } else if (!listType.value().isAssignableFrom(evaluatedItem.type())) {
                listType = new ArrayTypeValue(PrimitiveType.ANY);
            }

            list.add(evaluatedItem);
        }

        if (listType == null) listType = new ArrayTypeValue(PrimitiveType.ANY);

        return new ArrayValue(listType, list);
    }

    @Override
    public Value visit(LiteralNode node) {
        return node.value();
    }

    @Override
    public Value visit(UnaryOperationNode node) {
        Value evaluatedOperand = node.operand().accept(this);

        return switch (node.operator()) {
            case MINUS -> evaluatedOperand.negative();
            case NOT -> evaluatedOperand.not();
            default -> throw new UnsupportedOperationException("Cannot evaluate unary operation. Invalid unary operator of token type '" + node.operator() + "'.");
        };

    }

    @Override
    public Value visit(BinaryOperationNode node) {
        Value left = node.leftOperand().accept(this);
        Value right = node.rightOperand().accept(this);

        return switch (node.operator()) {
            case PLUS -> left.add(right);
            case MINUS -> left.subtract(right);
            case STAR -> left.multiply(right);
            case SLASH -> left.divide(right);
            case PERCENT -> left.module(right);
            case CARET -> left.power(right);

            case AND -> left.and(right);
            case OR -> left.or(right);

            case UNION -> left.union(right);
            case INTERSECTION -> left.intersection(right);

            case IS -> left.is(right);

            case AS -> left.as(right);

            case EQUALS -> left.equalsTo(right);
            case NOT_EQUALS -> left.notEqualsTo(right);
            case LESS_THAN -> left.lessThan(right);
            case GREATER_THAN -> left.greaterThan(right);
            case LESS_THAN_OR_EQUALS -> left.lessThanOrEqualsTo(right);
            case GREATER_THAN_OR_EQUALS -> left.greaterThanOrEqualsTo(right);

            default -> throw new UnsupportedOperationException("Cannot evaluate binary operation. Invalid binary operator of token type '" + node.operator() + "'.");
        };
    }

    @Override
    public Value visit(IfExpressionNode node) {
        Value evaluatedCondition = node.condition().accept(this);

        if (evaluatedCondition.isTruthy().value()) {
            return node.pass().accept(this);
        }

        return node.fail().accept(this);
    }

    @Override
    public Value visit(IndexAccessNode node) {
        Value evaluatedList = node.list().accept(this);
        Value indexValue = node.index().accept(this);

        return evaluatedList.get(indexValue);
    }

    @Override
    public Value visit(StructBlockNode node) {
        Map<String, TypeValue> binding = new LinkedHashMap<>();

        for (ExpressionNode parent: node.parents()) {
            TypeValue parentType = evaluateToType(parent);
            if (!(parentType instanceof ClassValue(Map<String, TypeValue> parentBinding))) throw new RuntimeException("Cannot evaluate struct block. A struct can only extend a struct type");

            binding.putAll(parentBinding);
        }

        for (ParameterNode parameter: node.fields()) {
            binding.put(parameter.name(), evaluateToType(parameter.type()));
        }

        return new StructValue(binding);
    }

    @Override
    public Value visit(EnumBlockNode node) {
        return new EnumValue(node.names());
    }

    @Override
    public Value visit(TupleNode node) {
        List<Value> values = new ArrayList<>();
        List<TypeValue> types = new ArrayList<>();
        for (ExpressionNode value: node.values()) {
            Value evaluatedValue = value.accept(this);
            values.add(evaluatedValue);
            types.add(evaluatedValue.type());
        }

        TupleTypeValue tupleType = new TupleTypeValue(types);
        return new TupleValue(tupleType, values);
    }
}