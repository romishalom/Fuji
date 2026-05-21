package fuji.parser;

public record Token(TokenType type, String lexeme, int line) {
}
