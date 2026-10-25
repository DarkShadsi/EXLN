package exln;

import java.util.List;

public class ExlnFunction implements ExlnCallable{
    Stmt.Function declaration;
    Environment closure;
    boolean isInitializer;

    public ExlnFunction(Stmt.Function declaration, Environment closure) {
        this(declaration, closure, false);
    }

    public ExlnFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments, Token paren) {
        Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (ReturnException returnValue) {
            if (isInitializer) return closure.getAt(0, "this");
            return returnValue.value;
        }
        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }
    @Override
    public String toString() {
        return "<func " + declaration.name.lexeme + ">";
    }
    ExlnFunction bind(ExlnInstance instance) {
        Environment env = new Environment(closure);
        env.define("this", instance);
        return new ExlnFunction(declaration, env, isInitializer);
    }
}
