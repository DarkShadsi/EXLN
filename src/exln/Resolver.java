package exln;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    Interpreter interpreter;
    Stack<Map<String, Boolean>> scopes = new Stack<>();
    private enum FunctionType {
        NONE,
        FUNCTION
    }
    private FunctionType currentFunction = FunctionType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }
    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }
    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }
    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }
    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }
    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }
    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Main.error(expr.name, "Can't read local variable in its own local initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }
    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr arg : expr.arguments) {
            resolve(arg);
        }
        return null;
    }
    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }
    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.object);
        resolve(expr.value);
        return null;
    }
    @Override
    public Void visitThisExpr(Expr.This expr) {
        return null;
    }
    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        return null;
    }
    @Override
    public Void visitArrayLiteralExpr(Expr.ArrayLiteral expr) {
        for (Expr element: expr.elements) {
            resolve(element);
        }
        return null;
    }
    @Override
    public Void visitIndexExpr(Expr.Index expr) {
        resolve(expr.object);
        resolve(expr.index);
        return null;
    }
    @Override
    public Void visitIndexSetExpr(Expr.IndexSet expr) {
        resolve(expr.object);
        resolve(expr.index);
        resolve(expr.value);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        for (Stmt statement: stmt.statements) {
            resolve(statement);
        }
        endScope();
        return null;
    }
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch);
        }
        return null;
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        if (stmt.increment != null) {
            resolve(stmt.increment);
        }
        return null;
    }
    @Override
    public Void visitDoWhileStmt(Stmt.DoWhile stmt) {
        resolve(stmt.body);
        resolve(stmt.condition);
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
        resolve(stmt.subject);
        for (int i = 0; i < stmt.caseValues.size(); i++) {
            resolve(stmt.caseValues.get(i));
            for (Stmt statement: stmt.caseBodies.get(i)) {
                resolve(statement);
            }
        }
        if (stmt.defaultBody != null) {
            resolve(stmt.defaultBody);
        }
        return null;
    }
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name, true);
        define(stmt.name);

        FunctionType enclosingFunction = currentFunction;
        currentFunction = FunctionType.FUNCTION;
        beginScope();
        for (Token param : stmt.params) {
            declare(param);
            define(param);
        }
        resolve(stmt.body);
        endScope();
        currentFunction = enclosingFunction;
        return null;
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Main.error(stmt.keyword, "Can't return from top-level code.");
        }
        if (stmt.value != null) {
            resolve(stmt.value);
        }
        return null;
    }
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        declare(stmt.name);
        define(stmt.name);
        return null;
    }
    @Override
    public Void visitEnumStmt(Stmt.Enum stmt) {
        declare(stmt.name);
        define(stmt.name);
        return null;
    }

    // HELPERS
    private void beginScope() {
        scopes.push(new HashMap<>());
    }
    private void endScope() {
        scopes.pop();
    }
    private void declare(Token name) {
        declare(name, false);
    }
    private void declare(Token name, boolean allowDuplicate) {
        if (scopes.isEmpty()) return;
        Map<String, Boolean> scope = scopes.peek();
        if (!allowDuplicate && scope.containsKey(name.lexeme)) {
            Main.error(name, "Already a variable with this name in this scope.");
        }
        scope.put(name.lexeme, false);
    }
    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }
    private void resolve(Expr expr) {
        expr.accept(this);
    }
    public void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }
    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                int distance = scopes.size() - 1 - i;
                interpreter.resolve(expr, distance);
                return;
            }
        }
    }

}
