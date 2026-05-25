package fuji.parser;

import fuji.ast.*;
import fuji.ast.controlflow.*;
import fuji.ast.expression.*;
import fuji.util.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public ProgramNode parse() {
        return parseProgram();
    }

    public ProgramNode parseProgram() {
        List<StatementNode> statements = new ArrayList<>();
        while (!isEndOfFile()) {
            if (match(TokenType.SEMICOLON)) continue;
            statements.add(parseStatement());
        }

        return new ProgramNode(statements);
    }



    private StatementNode parseStatement() {
        StatementNode statement = switch (peek().type()) {
            case CONST, LET -> parseDeclaration();
            case BREAK -> BreakNode.INSTANCE;
            case CONTINUE -> ContinueNode.INSTANCE;
            case IF -> parseIfStatement();
            case RETURN -> parseReturnStatement();
            case WHILE -> parseWhileLoop();
            case DO -> parseDoWhileLoop();
            case FOR -> parseForLoop();

            default -> new ExpressionStatementNode(parseExpression());
        };

        match(TokenType.SEMICOLON);

        return statement;
    }

    private DeclarationNode parseDeclaration() {
        boolean isConstant = match(TokenType.CONST);
        if (!isConstant)
            match(TokenType.LET);

        ExpressionNode target = parseOr();

        ExpressionNode type = null;
        if (match(TokenType.COLON))
            type = parseOr();

        ExpressionNode value = null;
        if (match(TokenType.ASSIGN))
            value = parseExpression();

        return new DeclarationNode(isConstant, target, type, value);
    }

    private IfStatementNode parseIfStatement() {
        consume(TokenType.IF);

        ExpressionNode condition = parseExpression();

        StatementNode pass = parseStatement();

        StatementNode fail = PassNode.INSTANCE;
        if (match(TokenType.ELSE))
            fail = parseStatement();

        return new IfStatementNode(condition, pass, fail);
    }

    private ReturnStatementNode parseReturnStatement() {
        consume(TokenType.RETURN);
        ExpressionNode returned = parseExpression();

        return new ReturnStatementNode(returned);
    }

    private WhileLoopNode parseWhileLoop() {
        consume(TokenType.WHILE);

        ExpressionNode condition = parseExpression();

        StatementNode body = parseStatement();

        return new WhileLoopNode(condition, body, false);
    }

    private WhileLoopNode parseDoWhileLoop() {
        consume(TokenType.DO);

        StatementNode body = parseStatement();

        consume(TokenType.WHILE);

        ExpressionNode condition = parseExpression();

        return new WhileLoopNode(condition, body, true);
    }

    private ForLoopNode parseForLoop() {
        consume(TokenType.FOR);

        boolean parens = match(TokenType.LEFT_PARENTHESIS);

        String iterationSymbolName = consume(TokenType.IDENTIFIER).lexeme();

        consume(TokenType.IN);

        ExpressionNode iterable = parseExpression();

        if (parens) consume(TokenType.RIGHT_PARENTHESIS);

        StatementNode body = parseStatement();

        return new ForLoopNode(iterationSymbolName, iterable, body);
    }

    private ExpressionNode parseExpression() {
        return parseAssignment();
    }

    private ExpressionNode parseAssignment() {
        ExpressionNode target = parseOr();

        if (match(TokenType.ASSIGN)) {
            ExpressionNode value = parseAssignment();
            return new AssignmentNode(target, value);
        }

        return target;
    }

    private ExpressionNode parseOr() {
        ExpressionNode left = parseAnd();

        while (match(TokenType.OR)) {
            ExpressionNode right = parseAnd();
            left = new BinaryOperationNode(left, TokenType.OR, right);
        }

        return left;
    }

    private ExpressionNode parseAnd() {
        ExpressionNode left = parseEquality();

        while (match(TokenType.AND)) {
            ExpressionNode right = parseEquality();
            left = new BinaryOperationNode(left, TokenType.AND, right);
        }

        return left;
    }

    private ExpressionNode parseEquality() {
        ExpressionNode left = parseComparison();

        while (match(TokenType.EQUALS, TokenType.NOT_EQUALS)) {
            TokenType type = previous().type();
            ExpressionNode right = parseComparison();
            left = new BinaryOperationNode(left, type, right);
        }

        return left;
    }

    private ExpressionNode parseComparison() {
        ExpressionNode left = parseTypeSafety();

        while (match(TokenType.GREATER_THAN, TokenType.LESS_THAN, TokenType.GREATER_THAN_OR_EQUALS, TokenType.LESS_THAN_OR_EQUALS)) {
            TokenType type = previous().type();
            ExpressionNode right = parseTypeSafety();
            left = new BinaryOperationNode(left, type, right);
        }

        return left;
    }

    private ExpressionNode parseTypeSafety() {
        ExpressionNode left = parseTypeIntersection();

        while (match(TokenType.AS, TokenType.IS)) {
            TokenType type = previous().type();
            ExpressionNode right = parseTerm();
            left = new BinaryOperationNode(left, type, right);
        }

        return left;
    }

    private ExpressionNode parseTypeIntersection() {
        ExpressionNode left = parseTypeUnion();

        while (match(TokenType.UNION)) {
            ExpressionNode right = parseTypeUnion();

            left = new BinaryOperationNode(left, TokenType.UNION, right);
        }

        return left;
    }

    private ExpressionNode parseTypeUnion() {
        ExpressionNode left = parseTerm();

        while (match(TokenType.INTERSECTION)) {
            ExpressionNode right = parseTerm();

            left = new BinaryOperationNode(left, TokenType.INTERSECTION, right);
        }

        return left;
    }

    private ExpressionNode parseTerm() {
        ExpressionNode left = parseFactor();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            TokenType type = previous().type();
            ExpressionNode right = parseFactor();
            left = new BinaryOperationNode(left, type, right);
        }

        return left;
    }

    private ExpressionNode parseFactor() {
        ExpressionNode left = parsePower();

        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            TokenType type = previous().type();
            ExpressionNode right = parseFactor();
            left = new BinaryOperationNode(left, type, right);
        }

        return left;
    }

    private ExpressionNode parsePower() {
        ExpressionNode left = parseAccessAndCall();

        if (match(TokenType.CARET)) {
            ExpressionNode right = parsePower();

            return new BinaryOperationNode(left, TokenType.CARET, right);
        }

        return left;
    }

    private ExpressionNode parseAccessAndCall() {
        ExpressionNode parent = parseUnaryOperation();

        while (check(TokenType.DOT, TokenType.LEFT_PARENTHESIS, TokenType.LEFT_SQUARE_BRACKET)) {
            if (match(TokenType.DOT)) {
                String name = consume(TokenType.IDENTIFIER).lexeme();
                parent = new AccessNode(parent, name);
            } else if (match(TokenType.LEFT_PARENTHESIS)) {
                List<ExpressionNode> args = new ArrayList<>();
                if (!check(TokenType.RIGHT_PARENTHESIS)) {
                    do {
                        args.add(parseExpression());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RIGHT_PARENTHESIS);
                parent = new CallNode(parent, args);
            } else if (match(TokenType.LEFT_SQUARE_BRACKET)) {
                ExpressionNode index = parseExpression();
                consume(TokenType.RIGHT_SQUARE_BRACKET);
                parent = new IndexAccessNode(parent, index);
            }
        }
        return parent;
    }

    private ExpressionNode parseUnaryOperation() {
        if (match(TokenType.MINUS, TokenType.NOT)) {
            TokenType type = previous().type();
            return new UnaryOperationNode(type, parseUnaryOperation());
        }

        return parsePrimary();
    }

    /*private ExpressionNode parseListType() {
        ExpressionNode expression = parsePrimary();

        if (peek().type() == TokenType.LEFT_SQUARE_BRACKET) {
            if (peekNext().type() == TokenType.RIGHT_SQUARE_BRACKET) {
                while (peek().type() == TokenType.LEFT_SQUARE_BRACKET && peekNext().type() == TokenType.RIGHT_SQUARE_BRACKET) {
                    advance();
                    advance();
                    expression = new ListTypeNode(expression);
                }
            } else {
                consume(TokenType.LEFT_SQUARE_BRACKET);
                do {

                }
            }
        }

        return expression;
    }*/

    private ExpressionNode parsePrimary() {
        Token peek = advance();
        return switch (peek.type()) {
            case INT_LITERAL -> new LiteralNode(new IntValue(Integer.parseInt(peek.lexeme())));
            case FLOAT_LITERAL -> new LiteralNode(new FloatValue(Double.parseDouble(peek.lexeme())));
            case STRING_LITERAL -> new LiteralNode(new StringValue(peek.lexeme()));
            case IDENTIFIER -> new ReferenceNode(peek.lexeme());

            case LEFT_PARENTHESIS -> {
                int saved = current;

                ExpressionNode grouped = null;
                if (!check(TokenType.RIGHT_PARENTHESIS)) {
                    grouped = parseExpression();

                    if (check(TokenType.COLON)) {
                        current = saved;
                        yield parseFunction();
                    } else if (check(TokenType.COMMA)) {
                        current = saved;
                        yield parseTuple();
                    }
                }

                consume(TokenType.RIGHT_PARENTHESIS);

                if (check(TokenType.COLON, TokenType.ARROW)) {
                    current = saved;
                    yield parseFunction();
                }

                if (grouped == null) throw new ParsingError("Cannot parse empty '()' as a standalone expression.");
                yield grouped;
            }

            case LESS_THAN -> {
                List<ExpressionNode> values = new ArrayList<>();

                if (!check(TokenType.GREATER_THAN)) {
                    do {
                        values.add(parseTypeSafety());
                    } while (match(TokenType.COMMA));
                }

                consume(TokenType.GREATER_THAN);

                yield new TupleNode(values);
            }

            case IF -> {
                ExpressionNode condition = parseExpression();

                ExpressionNode pass = parseExpression();

                ExpressionNode fail = new LiteralNode(EmptyValue.NONE);
                if (match(TokenType.ELSE))
                    fail = parseOr();

                yield new IfExpressionNode(condition, pass, fail);
            }

            case LEFT_CURLY_BRACKET -> {
                List<StatementNode> body = new ArrayList<>();

                while (!match(TokenType.RIGHT_CURLY_BRACKET)) {
                    body.add(parseStatement());
                }

                yield new BlockNode(body);
            }

            case OBJECT -> {
                List<ExpressionNode> parents = parseExtends();
                List<PropertyNode> properties = parseProperties();

                yield new ObjectBlockNode(parents, properties);
            }

            case STRUCT -> {
                consume(TokenType.LEFT_CURLY_BRACKET);
                List<ParameterNode> constructorParameters = new ArrayList<>();
                if (!check(TokenType.RIGHT_PARENTHESIS)) {
                    do {
                        constructorParameters.add(parseParameter());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RIGHT_PARENTHESIS);

                List<ExpressionNode> parents = parseExtends();
                List<PropertyNode> properties = parseProperties();

                yield new StructBlockNode(constructorParameters, parents, properties);
            }

            case INTERFACE -> {
                List<ExpressionNode> parents = parseExtends();

                consume(TokenType.LEFT_CURLY_BRACKET);
                List<ParameterNode> fields = new ArrayList<>();
                while (!match(TokenType.RIGHT_CURLY_BRACKET)) {
                    fields.add(parseParameter());
                }

                yield new InterfaceBlockNode(parents, fields);
            }

            case ENUM -> {
                consume(TokenType.LEFT_CURLY_BRACKET);
                List<String> names = new ArrayList<>();

                while (!match(TokenType.RIGHT_CURLY_BRACKET)) {
                    names.add(consume(TokenType.IDENTIFIER).lexeme());
                    match(TokenType.COMMA);
                }

                yield new EnumBlockNode(names);
            }

            case LEFT_SQUARE_BRACKET -> {
                List<ExpressionNode> listLiteral = new ArrayList<>();

                if (!check(TokenType.RIGHT_SQUARE_BRACKET)) {
                    do {
                        listLiteral.add(parseExpression());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RIGHT_SQUARE_BRACKET);

                yield new ArrayNode(null, listLiteral);
            }

            default -> throw new ParsingError("Unknown expression type. Got token type '" + peek.type() + "' at token index " + (current-1) + " (line: " + peek.line() + ")");
        };
    }

    private ParameterNode parseParameter() {
        String name = consume(TokenType.IDENTIFIER).lexeme();
        ExpressionNode type = new LiteralNode(PrimitiveType.ANY);
        if (match(TokenType.COLON))
            type = parseExpression();

        return new ParameterNode(name, type);
    }

    private List<ExpressionNode> parseExtends() {
        List<ExpressionNode> parents = new ArrayList<>();
        if (match(TokenType.EXTENDS)) {
            do {
                parents.add(parseExpression());
            } while (match(TokenType.COMMA));
        }

        return parents;
    }

    private List<PropertyNode> parseProperties() {
        List<PropertyNode> properties = new ArrayList<>();
        while (!match(TokenType.RIGHT_CURLY_BRACKET)) {
            properties.add(parseProperty());
        }

        return properties;
    }

    private PropertyNode parseProperty() {
        String name = consume(TokenType.IDENTIFIER).lexeme();
        ExpressionNode type = null;
        if (match(TokenType.COLON))
            type = parseExpression();

        ExpressionNode value = null;
        if (match(TokenType.ASSIGN))
            value = parseExpression();

        return new PropertyNode(name, type, value);
    }

    private FunctionNode parseFunction() {
        List<ParameterNode> parameters = new ArrayList<>();
        boolean vararg = false;
        if (!check(TokenType.RIGHT_PARENTHESIS)) {
            do {
                parameters.add(parseParameter());
            } while (match(TokenType.COMMA));

            vararg = match(TokenType.VARARG);
        }

        consume(TokenType.RIGHT_PARENTHESIS);

        ExpressionNode returnType = new LiteralNode(PrimitiveType.ANY);
        if (match(TokenType.COLON)) {
            returnType = parseExpression();
        }

        consume(TokenType.ARROW);

        ExpressionNode body = parseExpression();

        return new FunctionNode(parameters, vararg, returnType, body);
    }

    private TupleNode parseTuple() {
        List<ExpressionNode> values = new ArrayList<>();

        if (!check(TokenType.RIGHT_PARENTHESIS)) {
            do {
                values.add(parseTypeSafety());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PARENTHESIS);

        return new TupleNode(values);
    }

    private boolean match(TokenType... types) {
        boolean check = check(types);
        if (check) advance();
        return check;
    }

    private boolean check(TokenType t) {
        return tokens.get(current).type() == t;
    }

    private boolean check(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) {
                return true;
            }
        }

        return false;
    }

    private Token advance() {
        if (!isEndOfFile()) current++;
        return previous();
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token peekNext() {
        return tokens.get(current+1);
    }

    private Token consume(TokenType t) {
        if(check(t)) return advance();

        throw new ParsingError("Expected token type '" + t.name() + "'. Got token type '" + peek().type() + "' at token index " + current + " (line: " + peek().line() + ")");
    }

    private boolean isEndOfFile() {
        return check(TokenType.END_OF_FILE);
    }
}
