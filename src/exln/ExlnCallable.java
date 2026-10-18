package exln;

import java.util.List;

public interface ExlnCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments, Token paren);
}
