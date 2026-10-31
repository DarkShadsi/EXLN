package exln;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lexer {
    int start;
    int current;
    int line;
    String source;
    List<Token> tokens;
    static Map<String, TokenType> reservedWords = Map.ofEntries(
            Map.entry("and", TokenType.AND),
            Map.entry("enum", TokenType.ENUM),
            Map.entry("return", TokenType.RETURN),
            Map.entry("break", TokenType.BREAK),
            Map.entry("false", TokenType.FALSE),
            Map.entry("super", TokenType.SUPER),
            Map.entry("case", TokenType.CASE),
            Map.entry("for", TokenType.FOR),
            Map.entry("switch", TokenType.SWITCH),
            Map.entry("class", TokenType.CLASS),
            Map.entry("func", TokenType.FUNC),
            Map.entry("this", TokenType.THIS),
            Map.entry("continue", TokenType.CONTINUE),
            Map.entry("if", TokenType.IF),
            Map.entry("true", TokenType.TRUE),
            Map.entry("default", TokenType.DEFAULT),
            Map.entry("null", TokenType.NULL),
            Map.entry("var", TokenType.VAR),
            Map.entry("do", TokenType.DO),
            Map.entry("or", TokenType.OR),
            Map.entry("while", TokenType.WHILE),
            Map.entry("else", TokenType.ELSE),
            Map.entry("println", TokenType.PRINTLN),
            Map.entry("print", TokenType.PRINT)

    );

    public Lexer(String source) {
        this.source = source;
        tokens = new ArrayList<>();
        start = 0;
        current = 0;
        line = 1;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;

            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));

        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            // SINGLE CHARACTERS TOKENS
            case '(':
                tokens.add(new Token(TokenType.LEFT_PAREN, "(", null, line));
                break;
            case ')':
                tokens.add(new Token(TokenType.RIGHT_PAREN, ")", null, line));
                break;
            case '[':
                tokens.add(new Token(TokenType.LEFT_BRACKET, "[", null, line));
                break;
            case ']':
                tokens.add(new Token(TokenType.RIGHT_BRACKET, "]", null, line));
                break;
            case  '{':
                tokens.add(new Token(TokenType.LEFT_BRACE, "{", null, line));
                break;
            case '}':
                tokens.add(new Token(TokenType.RIGHT_BRACE, "}", null, line));
                break;
            case '.':
                tokens.add(new Token(TokenType.DOT, ".", null, line));
                break;
            case ',':
                tokens.add(new Token(TokenType.COMMA, ",", null, line));
                break;
            case ';':
                tokens.add(new Token(TokenType.SEMICOLON, ";", null, line));
                break;
            case ':':
                tokens.add(new Token(TokenType.COLON, ":", null, line));
                break;
            case  '+':
                tokens.add(new Token(TokenType.PLUS, "+", null, line));
                break;
            case '-':
                tokens.add(new Token(TokenType.MINUS, "-", null, line));
                break;
            case '*':
                tokens.add(new Token(TokenType.STAR, "*", null, line));
                break;

            // ONE OR TWO CHARACTER TOKENS
            case '=':
                if (match('=')) {
                    tokens.add(new Token(TokenType.EQUAL_EQUAL, "==", null, line));
                } else {
                    tokens.add(new Token(TokenType.EQUAL, "=", null, line));
                }
                break;
            case '!':
                if (match('=')) {
                    tokens.add(new Token(TokenType.BANG_EQUAL, "!=", null, line));
                } else {
                    tokens.add(new Token(TokenType.BANG, "!", null, line));
                }
                break;
            case '>':
                if (match('=')) {
                    tokens.add(new Token(TokenType.GREATER_EQUAL, ">=", null, line));
                } else {
                    tokens.add(new Token(TokenType.GREATER, ">", null, line));
                }
                break;
            case '<':
                if (match('=')) {
                    tokens.add(new Token(TokenType.LESS_EQUAL, "<=", null, line));
                } else {
                    tokens.add(new Token(TokenType.LESS, "<", null, line));
                }
                break;
            case '/':
                if (match('/')) {
                    comment();
                } else {
                    tokens.add(new Token(TokenType.SLASH, "/", null, line));
                }
                break;

            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;

            // LITERALS
            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Main.error(line, "Unexpected character '" + c + "'");
                }

        }

    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char peek() {
        if (current >= source.length()) {
            return '\0';
        }

        return source.charAt(current);
    }

    private char peekNext() {
        if (current+1 >= source.length()) {
            return '\0';
        }

        return source.charAt(current + 1);
    }

    private boolean match(char expected) {
        if (peek() == expected) {
            advance();
            return true;
        } else {
            return false;
        }
    }

    private void string() {
        StringBuilder value = new StringBuilder();

        while (!isAtEnd() && peek() != '"') {
            char c = advance();
            if (c == '\n') line++;

            if (c == '\\') {
                if (isAtEnd()) break;               // the file ends right after the backslash
                char next = advance();
                if (next == '\n') line++;
                switch (next) {
                    case 'n':  value.append('\n'); break;
                    case 't':  value.append('\t'); break;
                    case '"':  value.append('"');  break;
                    case '\\': value.append('\\'); break;
                    default:
                        Main.error(line, "Unknown escape sequence '\\" + next + "'.");
                }
            } else {
                value.append(c);
            }
        }

        if (isAtEnd()) {
            Main.error(line, "Unterminated string");
            return;
        }

        advance();                                   // the closing quote
        String lexeme = source.substring(start, current);   // the text as written, with the quotes
        tokens.add(new Token(TokenType.STRING, lexeme, value.toString(), line));
    }

    private void number() {
        while (!isAtEnd() && isDigit(peek())) {
            advance();
        }

        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (!isAtEnd() && isDigit(peek())) {
                advance();
            }
        }

        String numberLexeme =  source.substring(start, current);
        double number = Double.parseDouble(numberLexeme);
        tokens.add(new Token(TokenType.NUMBER, numberLexeme, number, line));
    }

    private void identifier() {
        while (!isAtEnd() && isAlphaNumeric(peek())) {
            advance();
        }

        String identifier = source.substring(start, current);

        if (reservedWords.containsKey(identifier)) {
            tokens.add(new Token(reservedWords.get(identifier), identifier, null, line));
        } else {
            tokens.add(new Token(TokenType.IDENTIFIER, identifier, identifier, line));
        }
    }

    private void comment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    private boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || c == '_' || isDigit(c);
    }

}
