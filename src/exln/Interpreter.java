package exln;

import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Environment environment = new Environment();
    public Interpreter() {
        environment.define("toNumber", new ExlnCallable() {
            @Override
            public int arity() {
                return 1;
            }
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments, Token paren) {
                Object x = arguments.getFirst();

                if (x instanceof Number) {
                    return x;
                } else if (x instanceof Boolean) {
                    return ((Boolean) x) ? 1.0 : 0.0;
                } else if (x instanceof String) {
                    String str = ((String) x).trim();
                    if (!str.matches("-?[0-9]+(\\.[0-9]+)?")) {
                        throw new RuntimeError(paren, "Non-numeric string cannot be parsed to a number.");
                    }
                    try {
                        return Double.parseDouble(str);
                    } catch (NumberFormatException e) {
                        throw new RuntimeError(paren, "Non-numeric string cannot be parsed to a number.");
                    }
                }
                throw new RuntimeError(paren, "Argument to toNumber() must be a Number, String, or Boolean.");
            }
        });
        environment.define("toString", new ExlnCallable() {
            @Override
            public int arity() {
                return 1;
            }
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments, Token paren) {
                return stringify(arguments.getFirst());
            }
        });
        environment.define("toBoolean", new ExlnCallable() {
            @Override
            public int arity() {
                return 1;
            }
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments, Token paren) {
                Object x = arguments.getFirst();

                if (x instanceof Double) {
                    double val = (double) x;
                    return val != 0.0;
                }
                throw new RuntimeError(paren, "Argument to toBoolean() must be a Number.");
            }
        });
    }
    public void interpret(List<Stmt> statements) {

        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Main.runtimeError(error);
        } catch (StackOverflowError error) {
            Main.runtimeError(new RuntimeError(null, "Stack overflow."));
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
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr arg : expr.arguments) {
            arguments.add(evaluate(arg));
        }

        ExlnCallable function;
        if (callee instanceof OverloadSet set) {
            function = set.find(arguments.size());
            if (function == null) {
                throw new RuntimeError(expr.paren, "No overload of '" + set.name + "' takes " + arguments.size() + " arguments.");
            }
        } else if (callee instanceof ExlnCallable) {
            function = (ExlnCallable) callee;
            if (arguments.size() != function.arity()) {
                throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
            }
        } else {
            throw new RuntimeError(expr.paren, "Can only call functions.");
        }

        return function.call(this, arguments, expr.paren);
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
        executeBlock(stmt.statements, new Environment(environment));
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
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        Object condition = evaluate(stmt.condition);
        if (isTruthy(condition)) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (BreakException e) {
                return null;
            } catch (ContinueException _) {
            }
            if (stmt.increment != null) execute(stmt.increment);
        }
        return null;
    }
    @Override
    public Void visitDoWhileStmt(Stmt.DoWhile stmt) {
        do {
            try {
                execute(stmt.body);
            } catch (BreakException e) {
                return null;
            } catch (ContinueException _) {}
        } while (isTruthy(evaluate(stmt.condition)));
        return null;
    }
    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakException();
    }
    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ContinueException();
    }
    @Override
    public Void visitSwitchStmt(Stmt.Switch stmt) {
        Object subject = evaluate(stmt.subject);
        int i = 0;
        for (Expr caseValue: stmt.caseValues) {
            Object value = evaluate(caseValue);
            if (isEqual(subject, value)) {
                for (Stmt body: stmt.caseBodies.get(i)) {
                    execute(body);
                }
                return null;
            }
            i++;
        }

        if (stmt.defaultBody != null) {
            for (Stmt body: stmt.defaultBody) {
                execute(body);
            }
        }
        return null;
    }
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        ExlnFunction func = new ExlnFunction(stmt, environment);
        Object existing = environment.getLocal(stmt.name.lexeme);
        if (existing instanceof OverloadSet) {
            boolean added = ((OverloadSet) existing).add(func);
            if (!added) {
                throw new RuntimeError(stmt.name, "Function '" + stmt.name.lexeme + "' with " + stmt.params.size() + " parameters is already defined.");
            }
        } else {
            OverloadSet set = new OverloadSet(stmt.name.lexeme);
            set.add(func);
            environment.define(stmt.name.lexeme, set);
        }
        return null;
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        throw new ReturnException(value);
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
            if (d % 1 == 0) return String.valueOf((long) d);
        }
        return value.toString();
    }
    void executeBlock(List<Stmt> statements, Environment newEnv) {
        Environment current = this.environment;
        environment = newEnv;
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            environment = current;
        }
    }
}