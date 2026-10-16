package exln;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Main.runtimeError(error);
        }
    }
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    // EXPRESSIONS
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        return switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                yield -(double) right;
            }
            case BANG -> !isTruthy(right);
            default -> null;
        };

    }
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        // Always evaluate left first, then right
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        Token operator = expr.operator;

        switch (operator.type) {
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                throw new RuntimeError(operator, "Operands must be two numbers or two strings.");
            case MINUS:
                checkNumberOperands(operator, left, right);
                return (double) left - (double) right;
            case STAR:
                checkNumberOperands(operator, left, right);
                return (double) left * (double) right;
            case SLASH:
                checkNumberOperands(operator, left, right);
                if ((double) right == 0) {
                    throw new RuntimeError(operator, "Division by zero.");
                }
                return (double) left / (double) right;
            case GREATER:
                checkNumberOperands(operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(operator, left, right);
                return (double) left <= (double) right;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
        }

        return null; // unreachable
    }
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        // Evaluate the left side once, and short-circuit
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else { // AND
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        return null;
    }
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return null;
    }
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        return null;
    }
    @Override
    public Object visitGetExpr(Expr.Get expr) {
        return null;
    }
    @Override
    public Object visitSetExpr(Expr.Set expr) {
        return null;
    }
    @Override
    public Object visitThisExpr(Expr.This expr) {
        return null;
    }
    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        return null;
    }
    @Override
    public Object visitArrayLiteralExpr(Expr.ArrayLiteral expr) {
        return null;
    }
    @Override
    public Object visitIndexExpr(Expr.Index expr) {
        return null;
    }
    @Override
    public Object visitIndexSetExpr(Expr.IndexSet expr) {
        return null;
    }

    // STATEMENTS
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        return null;
    }
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        String valString = stringify(value);
        System.out.println("EXLN:: " + valString);
        return null;
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        return null;
    }
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        return null;
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        return null;
    }
    @Override
    public Void visitDoWhileStmt(Stmt.DoWhile stmt) {
        return null;
    }
    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }
    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        return null;
    }
    @Override
    public Void visitSwitchStmt(Stmt.Switch stmt) {
        return null;
    }
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        return null;
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        return null;
    }
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        return null;
    }
    @Override
    public Void visitEnumStmt(Stmt.Enum stmt) {
        return null;
    }

    // HELPERS
    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (boolean) value;
        return true;
    }
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
    private String stringify (Object value) {
        if (value == null) return "null";
        if (value instanceof Double) {
            double  d = (double) value;
            if (d % 1 == 0) return String.valueOf((long) d);;
        }
        return value.toString();
    }
}