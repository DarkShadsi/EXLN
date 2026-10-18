package exln;

import java.util.List;

public class ExlnFunction implements ExlnCallable{
    Stmt.Function declaration;
    Environment closure;

    public ExlnFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
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
            return returnValue.value;
        }
        return null;
    }
    @Override
    public String toString() {
        return "<func " + declaration.name.lexeme + ">";
    }
}
