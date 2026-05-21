package fuji.parser;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final String input;
    private int pos = 0, line = 1;

    public Lexer(String input) {
        this.input = input;
    }

    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char current = peek();
            if (Character.isWhitespace(current)) {
                if (current == '\n') line++;
                advance();
            } else if (current == '/' && peekNext() == '/') {
                if (peekNext() == '/') {
                    while (peek() != '\n' && peek() != '\0') advance();
                }
            } else if (current == '/' && peekNext() == '*') {
                advance(); advance(); // Advance the '/*'
                while (peek() != '\0' && peek() != '*' && peekNext() != '/' && peekNext() != '\0') {
                    if (peek()=='\n') line++;
                    advance();
                }
                if (peek() == '\0' || peekNext() == '\0') throw new LexingError("Undetermined multiline comment. Expected '*/' after position " + pos + " (line: " + line + ")");
                advance(); advance(); // Advance the '*/'
            }else if (current == '"') {
                tokens.add(lexString());
            } else if (Character.isDigit(current)) {
                tokens.add(lexNumber());
            } else if (Character.isLetter(current) || current == '_' || current == '$') {
                tokens.add(lexIdentifier());
            } else {
                switch (current) {
                    case '&' -> tokens.add(peekNext() == '&' ? advanceTwo(TokenType.AND) : single(TokenType.UNION));
                    case '|' -> tokens.add(peekNext() == '|' ? advanceTwo(TokenType.OR) : single(TokenType.INTERSECTION));
                    case '+' -> tokens.add(single(TokenType.PLUS));
                    case '-' -> tokens.add(single(TokenType.MINUS));
                    case '*' -> tokens.add(single(TokenType.STAR));
                    case '/' -> tokens.add(single(TokenType.SLASH));
                    case '%' -> tokens.add(single(TokenType.PERCENT));
                    case '^' -> tokens.add(single(TokenType.CARET));
                    case '(' -> tokens.add(single(TokenType.LEFT_PARENTHESIS));
                    case ')' -> tokens.add(single(TokenType.RIGHT_PARENTHESIS));
                    case '[' -> tokens.add(single(TokenType.LEFT_SQUARE_BRACKET));
                    case ']' -> tokens.add(single(TokenType.RIGHT_SQUARE_BRACKET));
                    case '{' -> tokens.add(single(TokenType.LEFT_CURLY_BRACKET));
                    case '}' -> tokens.add(single(TokenType.RIGHT_CURLY_BRACKET));
                    case ',' -> tokens.add(single(TokenType.COMMA));
                    case ':' -> tokens.add(single(TokenType.COLON));
                    case ';' -> tokens.add(single(TokenType.SEMICOLON));
                    case '.' -> tokens.add(peekNext() == '.' && peekTwo() == '.' ? advanceThree(TokenType.VARARG) : single(TokenType.DOT));
                    case '=' -> tokens.add(peekNext() == '>' ? advanceTwo(TokenType.ARROW) : peekNext() == '=' ?  advanceTwo(TokenType.EQUALS) : single(TokenType.ASSIGN));
                    case '!' -> tokens.add(peekNext() == '=' ? advanceTwo(TokenType.NOT_EQUALS) : single(TokenType.NOT));
                    case '<' -> tokens.add(peekNext() == '=' ? advanceTwo(TokenType.LESS_THAN_OR_EQUALS) : single(TokenType.LESS_THAN));
                    case '>' -> tokens.add(peekNext() == '=' ? advanceTwo(TokenType.GREATER_THAN_OR_EQUALS) : single(TokenType.GREATER_THAN));
                    default -> throw new LexingError("Unexpected character '" + current + "' at position " + pos + " (line: " + line + ")." );
                }
            }
        }
        tokens.add(new Token(TokenType.END_OF_FILE, "", line));
        return tokens;
    }

    private char peek() { return pos < input.length() ? input.charAt(pos) : '\0'; }
    private char peekNext() { return pos + 1 < input.length() ? input.charAt(pos + 1) : '\0'; }
    private char peekTwo() { return pos + 2 < input.length() ? input.charAt(pos + 2) : '\0'; }

    private void advance() { pos++; }
    private Token single(TokenType type) { String val = String.valueOf(peek()); advance(); return new Token(type, val, line); }
    private Token advanceTwo(TokenType type) { String val = "" + peek() + peekNext(); pos += 2; return new Token(type, val, line); }

    private Token advanceThree(TokenType type) { String val = "" + peek() + peekNext() + peekTwo(); pos += 3; return new Token(type, val, line); }


    private Token lexString() {
        advance();
        int start = pos;
        while (peek() != '"' && peek() != '\0') {
            if (peek() == '\n') line++;
            advance();
        }
        if (peek() == '\0') throw new LexingError("Unterminated string. Expected '\"' after position " + pos + " (line: " + line + ").");
        String value = input.substring(start, pos);
        advance();
        return new Token(TokenType.STRING_LITERAL, value, line);
    }

    private Token lexNumber() {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(peek())) {
            sb.append(peek());
            advance();
        }
        if (peek()=='.') {
            sb.append(peek());
            advance();
            while (Character.isDigit(peek()) || peek()=='e' || peek()=='-') {
                sb.append(peek());
                advance();
            }

            return new Token(TokenType.FLOAT_LITERAL, sb.toString(), line);
        }

        return new Token(TokenType.INT_LITERAL, sb.toString(), line);
    }

    private Token lexIdentifier() {
        int start = pos;
        while (Character.isLetterOrDigit(peek()) || peek() == '_') advance();
        String text = input.substring(start, pos);
        return switch (text) {
            case "is" -> new Token(TokenType.IS, text, line);
            case "as" -> new Token(TokenType.AS, text, line);

            case "if" -> new Token(TokenType.IF, text, line);
            case "else" -> new Token(TokenType.ELSE, text, line);
            //case "true" -> new Token(TokenType.TRUE, text, line);
            //case "false" -> new Token(TokenType.FALSE, text, line);
            case "while" -> new Token(TokenType.WHILE, text, line);
            case "do" -> new Token(TokenType.DO, text, line);
            case "return" -> new Token(TokenType.RETURN, text, line);
            case "in" -> new Token(TokenType.IN, text, line);
            case "for" -> new Token(TokenType.FOR, text, line);
            case "const" -> new Token(TokenType.CONST, text, line);
            case "let" -> new Token(TokenType.LET, text, line);
            case "continue" -> new Token(TokenType.CONTINUE, text, line);
            case "break" -> new Token(TokenType.BREAK, text, line);
            case "object" -> new Token(TokenType.OBJECT, text, line);
            case "map" -> new Token(TokenType.MAP, text, line);
            case "enum" -> new Token(TokenType.ENUM, text, line);
            case "struct" -> new Token(TokenType.STRUCT, text, line);
            case "extends" -> new Token(TokenType.EXTENDS, text, line);

            //case "bool" -> new Token(TokenType.TYPE_BOOL, text, line);
            //case "int" -> new Token(TokenType.TYPE_INT, text, line);
            //case "float" -> new Token(TokenType.TYPE_FLOAT, text, line);
            //case "string" -> new Token(TokenType.TYPE_STRING, text, line);
            //case "any" -> new Token(TokenType.TYPE_ANY, text, line);
            //case "void" -> new Token(TokenType.TYPE_VOID, text, line);
            //case "empty" -> new Token(TokenType.TYPE_EMPTY, text, line);
            //case "type" -> new Token(TokenType.TYPE, text, line);

            //case "unit" -> new Token(TokenType.UNIT, text, line);
            //case "none" -> new Token(TokenType.NONE, text, line);

            default -> new Token(TokenType.IDENTIFIER, text, line);
        };
    }
}
