package exln;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExlnClass implements ExlnOverloaded {
    final String name;
    final ExlnClass superclass;
    final Map<String, OverloadSet> methods = new HashMap<>();

    ExlnClass(String name, ExlnClass superclass) {
        this.name = name;
        this.superclass = superclass;
    }

    boolean addMethod(String methodName, ExlnFunction fn) {
        OverloadSet set = methods.get(methodName);
        if (set == null) {
            set = new OverloadSet(methodName);
            methods.put(methodName, set);
        }
        return set.add(fn);
    }
    boolean hasMethod(String methodName) {
        if ( methods.containsKey(methodName) ) return true;
        if (superclass != null) { return superclass.hasMethod(methodName); }
        return false;
    }
    ExlnFunction findMethod(String methodName, int argCount) {
        OverloadSet set = methods.get(methodName);
        if (set != null) {
            ExlnFunction fn = set.find(argCount);
            if (fn != null) return fn;
        }
        if (superclass != null) {
            return superclass.findMethod(methodName, argCount);
        }
        return null;
    }
    @Override
    public String name() {
        return name;
    }
    @Override
    public ExlnCallable find(int argCount) {
        final ExlnClass klass = this;
        final ExlnFunction initializer = findMethod("init", argCount);

        if (initializer == null && (hasMethod("init") || argCount != 0)) {
            return null;
        }

        return new ExlnCallable() {
            @Override
            public int arity() {
                return argCount;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments, Token paren) {
                ExlnInstance instance = new ExlnInstance(klass);
                if (initializer != null) {
                    initializer.bind(instance).call(interpreter, arguments, paren);
                }
                return instance;
            }
        };
    }
    @Override
    public String toString() {
        return "<class " + name + ">";
    }
}