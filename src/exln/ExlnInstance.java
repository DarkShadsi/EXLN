package exln;

import java.util.HashMap;
import java.util.Map;

public class ExlnInstance {
    private final ExlnClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    ExlnInstance(ExlnClass klass) {
        this.klass = klass;
    }

    Object get(Token name) {
        if ( fields.containsKey(name.lexeme)) return fields.get(name.lexeme);
        if ( klass.hasMethod(name.lexeme)) return new BoundMethods(this, klass, name.lexeme);
        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}