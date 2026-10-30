package exln;

import java.util.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private final Environment globals = new Environment();
    private Environment environment = globals;
    private Map<Expr,Integer> locals = new HashMap<Expr,Integer>();

    public Interpreter() {
        globals.define("toNumber", new ExlnCallable() {
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
        globals.define("toString", new ExlnCallable() {
            @Override
            public int arity() {
                return 1;
            }
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments, Token paren) {
                return stringify(arguments.getFirst());
            }
        });
        globals.define("toBoolean", new ExlnCallable() {
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
        globals.define("length", new ExlnCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments, Token bracket) {
                Object x = arguments.getFirst();
                if (!(x instanceof ExlnArray)) {
                    throw new RuntimeError(bracket, "Argument must be an array.");
                }
                return (double) ((ExlnArray) x).getElements().length;
            }
        });
        globals.define("array", new ExlnCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments, Token paren) {
                Object sizeArg = arguments.getFirst();
                if (!(sizeArg instanceof Double)) {
                    throw new RuntimeError(paren, "Array size must be a number.");
                }

                double dSize = (Double) sizeArg;
                if (dSize % 1 != 0) {
                    throw new RuntimeError(paren, "Array size must be a whole number.");
                }

                if (dSize < 0) {
                    throw new RuntimeError(paren, "Array size must be non-negative.");
                }

                int size = (int) dSize;
                Object[] elements = new Object[size];

                return new ExlnArray(elements);
            }
        });
        globals.define("split", new ExlnCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments, Token paren) {
                Object text = arguments.get(0);
                Object separator = arguments.get(1);
                if (!(text instanceof String) || !(separator instanceof String)) {
                    throw new RuntimeError(paren, "split() needs two strings.");
                }

                String s = (String) text;
                String sep = (String) separator;
                if (sep.isEmpty()) {
                    throw new RuntimeError(paren, "Separator must not be empty.");
                }

                List<Object> parts = new ArrayList<>();
                int start = 0;
                int found;
                while ((found = s.indexOf(sep, start)) != -1) {
                    parts.add(s.substring(start, found));
                    start = found + sep.length();
                }
                parts.add(s.substring(start));

                return new ExlnArray(parts.toArray());
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
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr arg : expr.arguments) {
            arguments.add(evaluate(arg));
        }

        ExlnCallable function;
        if (callee instanceof ExlnOverloaded set) {
            function = set.find(arguments.size());
            if (function == null) {
                throw new RuntimeError(expr.paren, "No overload of '" + set.name() + "' takes " + arguments.size() + " arguments.");
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
        Object object = evaluate(expr.object);

        if (object instanceof ExlnInstance instance) {
            return instance.get(expr.name);
        }

        if (object instanceof ExlnEnum enumObj) {
            if (expr.name.lexeme.equals("values")) {
                return new ExlnCallable() {
                    @Override
                    public int arity() {
                        return 0;
                    }

                    @Override
                    public Object call(Interpreter interpreter, List<Object> arguments, Token paren) {
                        return new ExlnArray(enumObj.members.toArray(new Object[0]));
                    }
                };
            }
            if (enumObj.memberMap.containsKey(expr.name.lexeme)) {
                return enumObj.memberMap.get(expr.name.lexeme);
            }
            throw new RuntimeError(expr.name, "Enum '" + enumObj.name + "' has no member '" + expr.name.lexeme + "'.");
        }
        if (object instanceof ExlnEnumValue exlnEnumValue) {
            if (expr.name.lexeme.equals("order")) {
                return new ExlnCallable() {
                    @Override
                    public int arity() {
                        return 0;
                    }

                    @Override
                    public Object call(Interpreter interpreter, List<Object> arguments, Token paren) {
                        return (double) exlnEnumValue.ordinal;
                    }
                };
            }
            throw new RuntimeError(expr.name, "Enum value '" + exlnEnumValue + "' has no property '" + expr.name.lexeme + "'.");
        }
        throw new RuntimeError(expr.name, "Only enums and instances have properties.");
    }
    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);
        if (object instanceof ExlnEnum || object instanceof ExlnEnumValue) {
            throw new RuntimeError(expr.name, "Cannot assign to enum members.");
        }
        if (!(object instanceof ExlnInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }
        Object value = evaluate(expr.value);
        ((ExlnInstance) object).set(expr.name, value);
        return value;
    }
    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }
    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);    // the resolver (9.4) always records this
        ExlnClass superclass = (ExlnClass) environment.getAt(distance, "super");
        ExlnInstance instance = (ExlnInstance) environment.getAt(distance - 1, "this");
        if (!superclass.hasMethod(expr.methodName.lexeme)) throw new RuntimeError(expr.methodName,
            "Undefined property '" + expr.methodName.lexeme + "'.");
        return new BoundMethods(instance, superclass, expr.methodName.lexeme);
    }
    @Override
    public Object visitArrayLiteralExpr(Expr.ArrayLiteral expr) {
        Object[] elements = new  Object[expr.elements.size()];

        for (int i = 0; i < elements.length; i++) {
            elements[i] = evaluate(expr.elements.get(i));
        }

        return new ExlnArray(elements);
    }
    @Override
    public Object visitIndexExpr(Expr.Index expr) {
        Object obj = evaluate(expr.object);
        if (! (obj instanceof ExlnArray)) {
            throw new RuntimeError(expr.bracket, "Only arrays can be indexed.");
        }
        Object index = evaluate(expr.index);
        index = checkIndex((ExlnArray) obj, index, expr.bracket);
        return ((ExlnArray) obj).get((Integer) index);
    }
    @Override
    public Object visitIndexSetExpr(Expr.IndexSet expr) {
        Object obj = evaluate(expr.object);
        if (! (obj instanceof ExlnArray)) {
            throw new RuntimeError(expr.bracket, "Only arrays can be indexed.");
        }
        Object index = evaluate(expr.index);
        index = checkIndex((ExlnArray) obj, index, expr.bracket);
        Object value = evaluate(expr.value);
        ((ExlnArray)obj).set((Integer) index, value);
        return value;
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
        ExlnClass superclass = null;
        if (stmt.superclass != null) {
            Object value = evaluate(stmt.superclass);
            if (!(value instanceof ExlnClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
            }
            superclass = (ExlnClass) value;
        }

        environment.define(stmt.name.lexeme, null);
        Environment previous = environment;
        try {
            if (superclass != null) {
                environment = new Environment(environment);
                environment.define("super", superclass);
            }

            ExlnClass klass = new ExlnClass(stmt.name.lexeme, superclass);

            for (Stmt.Function method : stmt.methods) {
                boolean isInit = method.name.lexeme.equals("init");
                ExlnFunction fn = new ExlnFunction(method, environment, isInit);
                if (!klass.addMethod(method.name.lexeme, fn)) throw new RuntimeError(method.name,
                "Method '" + method.name.lexeme + "' with " + method.params.size()
                + " parameters is already defined in class '" + stmt.name.lexeme + "'.");
            }

            environment = previous;
            environment.assign(stmt.name, klass);
        } finally {
            environment = previous;
        }
        return null;
    }
    @Override
    public Void visitEnumStmt(Stmt.Enum stmt) {
        ExlnEnum enumObj = new ExlnEnum(stmt.name.lexeme);

        for (int i = 0; i < stmt.members.size(); i++) {
            Token token = stmt.members.get(i);
            if (token.lexeme.equals("values")) {
                throw new RuntimeError(token, "Enum '" + stmt.name.lexeme + "' cannot have a member named 'values'.");
            }
            ExlnEnumValue member = new ExlnEnumValue(enumObj, token.lexeme, i);
            if (!enumObj.addMember(member)) {
                throw new RuntimeError(token, "Duplicate enum member '" + token.lexeme + "'.");
            }
        }
        environment.define(stmt.name.lexeme, enumObj);
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
            double d = (double) value;
            if (d % 1 == 0) return String.valueOf((long) d);
        }
        if (value instanceof ExlnArray) {
            Object[] elements = ((ExlnArray) value).getElements();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < elements.length; i++) {
                sb.append(stringify(elements[i]));
                if (i < elements.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
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
    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }
    private Object lookUpVariable(Token name, Expr expr) {
        if (locals.containsKey(expr)) {
            return environment.getAt(locals.get(expr), name.lexeme);
        }
        return globals.get(name);
    }
    private int checkIndex(ExlnArray arr, Object index, Token bracket) {
        if (!(index instanceof Double)) {
            throw new RuntimeError(bracket, "Index must be a number.");
        }

        double dIndex = (Double) index;
        if (dIndex % 1 != 0) {
            throw new RuntimeError(bracket, "Array index must be a whole number.");
        }

        int intIndex = (int) dIndex;
        if (intIndex < 0 || intIndex >= arr.size()) {
            throw new RuntimeError(bracket, "Array index out of bounds.");
        }
        return intIndex;
    }
}