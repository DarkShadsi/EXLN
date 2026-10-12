package tools;

import exln.Expr;
import exln.Token;
import exln.TokenType;
import java.util.List;

public class ASTPrinter implements Expr.Visitor<String> {

    public static void main(String[] args) {
        // 1. The main test from the prompt: (* (- 123.0) (group 45.67))
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123.0)
                ),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)
                )
        );
        System.out.println("Binary/Unary/Grouping test:");
        System.out.println(new ASTPrinter().print(expression));

        // 2. Test a Call expression: call(print, 123)
        Expr callExpr = new Expr.Call(
                new Expr.Variable(new Token(TokenType.IDENTIFIER, "print", null, 1)),
                new Token(TokenType.LEFT_PAREN, "(", null, 1),
                List.of(new Expr.Literal(123.0))
        );
        System.out.println("\nCall test:");
        System.out.println(new ASTPrinter().print(callExpr));

        // 3. Test a Get expression: . (object, name)
        Expr getExpr = new Expr.Get(
                new Expr.Variable(new Token(TokenType.IDENTIFIER, "obj", null, 1)),
                new Token(TokenType.IDENTIFIER, "property", null, 1)
        );
        System.out.println("\nGet test:");
        System.out.println(new ASTPrinter().print(getExpr));

        // 4. Test an Index expression: index (object, index)
        Expr indexExpr = new Expr.Index(
                new Expr.Variable(new Token(TokenType.IDENTIFIER, "arr", null, 1)),
                new Token(TokenType.LEFT_BRACKET, "[", null, 1),
                new Expr.Literal(0.0)
        );
        System.out.println("\nIndex test:");
        System.out.println(new ASTPrinter().print(indexExpr));
    }

    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("=", expr.name, expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) {
            return "null";
        }
        return expr.value.toString();
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        Object[] parts = new Object[1 + expr.arguments.size()];
        parts[0] = expr.callee;
        for (int i = 0; i < expr.arguments.size(); i++) {
            parts[i + 1] = expr.arguments.get(i);
        }
        return parenthesize("call", parts);
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return parenthesize(".", expr.object, expr.name);
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return parenthesize("set", expr.object, expr.name, expr.value);
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return "this";
    }

    @Override
    public String visitSuperExpr(Expr.Super expr) {
        return parenthesize("super", expr.methodName);
    }

    @Override
    public String visitArrayLiteralExpr(Expr.ArrayLiteral expr) {
        Object[] parts = new Object[expr.elements.size()];
        for (int i = 0; i < expr.elements.size(); i++) {
            parts[i] = expr.elements.get(i);
        }
        return parenthesize("array", parts);
    }

    @Override
    public String visitIndexExpr(Expr.Index expr) {
        return parenthesize("index", expr.object, expr.index);
    }

    @Override
    public String visitIndexSetExpr(Expr.IndexSet expr) {
        return parenthesize("index=", expr.object, expr.index, expr.value);
    }

    private String parenthesize(String name, Object... parts) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Object part : parts) {
            builder.append(" ");
            if (part instanceof Expr) {
                builder.append(((Expr) part).accept(this));
            } else if (part instanceof Token) {
                builder.append(((Token) part).lexeme);
            } else {
                builder.append(part.toString());
            }
        }
        builder.append(")");
        return builder.toString();
    }
}