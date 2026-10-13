package exln;

import java.util.List;

public class ASTPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

    public String print(Expr expr) {
        return expr.accept(this);
    }

    public String print(Stmt stmt) {
        return stmt.accept(this);
    }

    // ===================== EXPRESSIONS =====================

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
        if (expr.value == null) return "null";
        return expr.value.toString();
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return parenthesize("call", expr.callee, expr.arguments);
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
        return parenthesize("array", expr.elements);
    }

    @Override
    public String visitIndexExpr(Expr.Index expr) {
        return parenthesize("index", expr.object, expr.index);
    }

    @Override
    public String visitIndexSetExpr(Expr.IndexSet expr) {
        return parenthesize("index=", expr.object, expr.index, expr.value);
    }

    // ===================== STATEMENTS =====================

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        return parenthesize("block", stmt.statements);
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return parenthesize("expr", stmt.expression);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return parenthesize("print", stmt.expression);
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        return parenthesize("var", stmt.name, stmt.initializer);
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        return parenthesize("if", stmt.condition, stmt.thenBranch, stmt.elseBranch);
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return parenthesize("while", stmt.condition, stmt.body, stmt.increment);
    }

    @Override
    public String visitDoWhileStmt(Stmt.DoWhile stmt) {
        return parenthesize("do", stmt.body, stmt.condition);
    }

    @Override
    public String visitBreakStmt(Stmt.Break stmt) {
        return "(break)";
    }

    @Override
    public String visitContinueStmt(Stmt.Continue stmt) {
        return "(continue)";
    }

    @Override
    public String visitSwitchStmt(Stmt.Switch stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(switch ").append(stmt.subject.accept(this));

        for (int i = 0; i < stmt.caseValues.size(); i++) {
            builder.append(" ").append(parenthesize("case", stmt.caseValues.get(i), stmt.caseBodies.get(i)));
        }

        if (stmt.defaultBody != null) {
            builder.append(" ").append(parenthesize("default", stmt.defaultBody));
        }

        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(func ").append(stmt.name.lexeme).append(" (");

        for (int i = 0; i < stmt.params.size(); i++) {
            if (i > 0) builder.append(" ");
            builder.append(stmt.params.get(i).lexeme);
        }
        builder.append(")");

        for (Stmt body : stmt.body) {
            builder.append(" ").append(body.accept(this));
        }

        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        return parenthesize("return", stmt.value);
    }

    @Override
    public String visitClassStmt(Stmt.Class stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(class ").append(stmt.name.lexeme);

        if (stmt.superclass != null) {
            builder.append(" < ").append(stmt.superclass.name.lexeme);
        }

        for (Stmt.Function method : stmt.methods) {
            builder.append(" ").append(method.accept(this));
        }

        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitEnumStmt(Stmt.Enum stmt) {
        return parenthesize("enum", stmt.name, stmt.members);
    }

    // ===================== HELPER =====================

    // Prints (name part1 part2 ...). Handles Expr, Stmt, Token, null, and lists of these.
    private String parenthesize(String name, Object... parts) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Object part : parts) {
            appendPart(builder, part);
        }
        builder.append(")");
        return builder.toString();
    }

    private void appendPart(StringBuilder builder, Object part) {
        if (part instanceof List<?> list) {
            for (Object item : list) {
                appendPart(builder, item);
            }
            return;
        }

        builder.append(" ");
        if (part == null) {
            builder.append("null");
        } else if (part instanceof Expr expr) {
            builder.append(expr.accept(this));
        } else if (part instanceof Stmt stmt) {
            builder.append(stmt.accept(this));
        } else if (part instanceof Token token) {
            builder.append(token.lexeme);
        } else {
            builder.append(part.toString());
        }
    }
}