package exln;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private List<Token> tokenList = new ArrayList<>();
    private int currentIndex = 0;

    private static class ParseError extends RuntimeException {
    }

    // ENTRY POINT
    public Expr parse(List<Token> tokenList) {
        this.tokenList = tokenList;
        this.currentIndex = 0;

        // Empty input: nothing to parse, and no error
        if (isAtEnd()) {
            return null;
        }

        try {
            Expr expr = expression();
            if (!isAtEnd()) {
                error(peek(), "Expect end of expression.");
                return null;
            }
            return expr;
        } catch (ParseError e) {
            return null;
        }
    }

    // HELPERS
    private Token peek() {
        return tokenList.get(currentIndex);
    }
    private Token previous() {
        return tokenList.get(currentIndex - 1);
    }
    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }
    private Token advance() {
        if (!isAtEnd()) currentIndex++;
        return previous();
    }
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }
    private ParseError error(Token token, String message) {
        Main.error(token, message);
        return new ParseError();
    }

    // EXPRESSIONS
    private Expr expression() {
        return assignment();
    }
    private Expr assignment() {
        Expr expr = logicOr();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable variable) {
                return new Expr.Assign(variable.name, value);
            } else if (expr instanceof Expr.Get get) {
                return new Expr.Set(get.object, get.name, value);
            } else if (expr instanceof Expr.Index idx) {
                return new Expr.IndexSet(idx.object, idx.bracket, idx.index, value);
            }

            // Reported, not thrown: the parser isn't confused, the target is just invalid
            error(equals, "Invalid assignment target.");
        }

        return expr;
    }
    private Expr logicOr() {
        Expr expr = logicAnd();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = logicAnd();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr logicAnd() {
        Expr expr = equality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr comparison() {
        Expr expr = term();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL,
                TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr term() {
        Expr expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr factor() {
        Expr expr = unary();

        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else if (match(TokenType.LEFT_BRACKET)) {
                Token bracket = previous();
                Expr index = expression();
                consume(TokenType.RIGHT_BRACKET, "Expect ']' after array index.");
                expr = new Expr.Index(expr, bracket, index);
            } else {
                break;
            }
        }
        return expr;
    }
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }
    private Expr primary() {
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.NULL)) return new Expr.Literal(null);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.THIS)) return new Expr.This(previous());

        if (match(TokenType.SUPER)) {
            Token keyword = previous();
            consume(TokenType.DOT, "Expect '.' after 'super'.");
            Token method = consume(TokenType.IDENTIFIER, "Expect superclass method name.");
            return new Expr.Super(keyword, method);
        }

        if (match(TokenType.IDENTIFIER)) return new Expr.Variable(previous());

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(TokenType.LEFT_BRACKET)) {
            List<Expr> elements = new ArrayList<>();
            if (!check(TokenType.RIGHT_BRACKET)) {
                do {
                    elements.add(expression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RIGHT_BRACKET, "Expect ']' after array elements.");
            return new Expr.ArrayLiteral(elements);
        }

        throw error(peek(), "Expect expression.");
    }
}