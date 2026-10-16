package exln;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private List<Token> tokenList = new ArrayList<>();
    private int currentIndex = 0;
    private int loopDepth = 0;

    private static class ParseError extends RuntimeException {
    }

    // ENTRY POINT
    public List<Stmt> parse(List<Token> tokenList) {
        this.tokenList = tokenList;
        this.currentIndex = 0;
        this.loopDepth = 0;

        List<Stmt> statements = new ArrayList<>();

        while (!isAtEnd()) {
            Stmt declr = declaration();
            if (declr != null) {
                statements.add(declr);
            }

        }

        return statements;
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
    private void synchronize() {
        advance();
        while(!isAtEnd()) {
            if(previous().type == TokenType.SEMICOLON) return;
            switch (peek().type) {
                case CLASS:
                case FUNC:
                case VAR:
                case ENUM:
                case FOR:
                case IF:
                case WHILE:
                case DO:
                case SWITCH:
                case PRINT:
                case RETURN:
                case BREAK:
                case CONTINUE:
                    return;
            }
            advance();
        }
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

    // STATEMENTS
    private Stmt declaration() {
        try{
            if (match(TokenType.CLASS)) return classDeclaration();
            if (match(TokenType.FUNC)) return function("function");
            if (match(TokenType.ENUM)) return enumDeclaration();
            if (match(TokenType.VAR)) return varDeclaration();

            return statement();
        } catch (ParseError e) {
            synchronize();
            return null;
        }
    }
    private Stmt varDeclaration() {
        Token identifier = consume(TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if(match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");

        return new Stmt.Var(identifier, initializer);
    }
    private Stmt enumDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect enum name.");
        consume(TokenType.LEFT_BRACE, "Expect '{' for enum declaration.");

        List<Token> members = new ArrayList<>();
        do {
            members.add(consume(TokenType.IDENTIFIER, "Expect enum member name."));
        } while (match(TokenType.COMMA));

        consume(TokenType.RIGHT_BRACE, "Expect '}' after enum declaration.");
        return new Stmt.Enum(name, members);
    }
    private Stmt.Function function(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");

        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");

        int enclosingLoopDepth = this.loopDepth;
        this.loopDepth = 0;
        try {
            List<Stmt> body = block();
            return new Stmt.Function(name, parameters, body);
        } finally {
            this.loopDepth = enclosingLoopDepth;
        }
    }
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            Stmt declr = declaration();
            if (declr != null) {
                statements.add(declr);
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }
    private Stmt classDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect class name.");

        Expr.Variable superclass = null;
        if (match(TokenType.LESS)) {
            Token superclassToken = consume(TokenType.IDENTIFIER, "Expect superclass name.");
            superclass = new Expr.Variable(superclassToken);
        }

        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");
        return new Stmt.Class(name, superclass, methods);
    }
    private Stmt statement() {
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.SWITCH)) return switchStatement();
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.DO)) return doWhileStatement();
        if (match(TokenType.BREAK)) return breakStatement();
        if (match(TokenType.CONTINUE)) return continueStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }
    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }
    private Stmt breakStatement() {
        Token keyword = previous();
        if (loopDepth == 0) {
            error(keyword, "Cannot use 'break' outside of a loop.");
        }
        consume(TokenType.SEMICOLON, "Expect ';' after 'break'.");
        return new Stmt.Break(keyword);
    }
    private Stmt continueStatement() {
        Token keyword = previous();
        if (loopDepth == 0) {
            error(keyword, "Cannot use 'continue' outside of a loop.");
        }
        consume(TokenType.SEMICOLON, "Expect ';' after 'continue'.");
        return new Stmt.Continue(keyword);
    }
    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;

        if (match(TokenType.ELSE)) {
            if (!(thenBranch instanceof Stmt.Block)) {
                error(previous(), "'if' body must use braces when followed by 'else'.");
            }

            if (!check(TokenType.LEFT_BRACE) && !check(TokenType.IF)) {
                error(peek(), "'else' body must be a block or an 'if'.");
            }

            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");

        loopDepth++;
        Stmt body;
        try {
            body = statement();
        } finally {
            loopDepth--;
        }

        return new Stmt.While(condition, body, null);
    }
    private Stmt doWhileStatement() {
        consume(TokenType.LEFT_BRACE, "Expect '{' before 'do' body.");

        loopDepth++;
        List<Stmt> bodyStatements;
        try {
            bodyStatements = block();
        } finally {
            loopDepth--;
        }
        Stmt body = new Stmt.Block(bodyStatements);

        consume(TokenType.WHILE, "Expect 'while' after 'do' body.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");

        consume(TokenType.SEMICOLON, "Expect ';' after 'do-while' loop.");

        return new Stmt.DoWhile(body, condition);
    }
    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

        Stmt.Expression increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = new Stmt.Expression(expression());
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

        loopDepth++;
        Stmt body;
        try {
            body = statement();
        } finally {
            loopDepth--;
        }

        if (condition == null) {
            condition = new Expr.Literal(true);
        }

        Stmt whileStmt = new Stmt.While(condition, body, increment);

        if (initializer != null) {
            List<Stmt> blockStatements = new ArrayList<>();
            blockStatements.add(initializer);
            blockStatements.add(whileStmt);
            return new Stmt.Block(blockStatements);
        }

        return whileStmt;
    }
    private Stmt switchStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'switch'.");
        Expr subject = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after switch subject.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before switch body.");

        List<Expr> caseValues = new ArrayList<>();
        List<List<Stmt>> caseBodies = new ArrayList<>();
        List<Stmt> defaultBody = null;

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.CASE)) {
                Expr value = expression();
                consume(TokenType.COLON, "Expect ':' after case value.");

                List<Stmt> body = new ArrayList<>();
                while (!check(TokenType.CASE) && !check(TokenType.DEFAULT) && !check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                    body.add(statement());
                }

                caseValues.add(value);
                caseBodies.add(body);

            } else if (match(TokenType.DEFAULT)) {
                consume(TokenType.COLON, "Expect ':' after 'default'.");

                defaultBody = new ArrayList<>();
                while (!check(TokenType.CASE) && !check(TokenType.DEFAULT) && !check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                    defaultBody.add(statement());
                }

            } else {
                error(peek(), "Expect 'case' or 'default' inside switch block.");
                advance();
            }
        }

        // 4. Consume the closing right brace '}'
        consume(TokenType.RIGHT_BRACE, "Expect '}' after switch body.");

        // 5. Build and return the Switch statement AST node
        // (Adjust parameters based on your specific Stmt.Switch constructor layout)
        return new Stmt.Switch(subject, caseValues, caseBodies, defaultBody);
    }
}