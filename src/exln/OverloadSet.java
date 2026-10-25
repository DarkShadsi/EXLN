package exln;

import java.util.HashMap;
import java.util.Map;

public class OverloadSet implements ExlnOverloaded {
    String name;
    Map<Integer, ExlnFunction> funcs = new HashMap<>();
    public OverloadSet(String name) {
        this.name = name;
    }
    public boolean add(ExlnFunction fn) {
        if (funcs.containsKey(fn.arity())) {
            return false;
        }
        funcs.put(fn.arity(), fn);
        return true;
    }
    @Override
    public String name() {
        return name;
    }
    @Override
    public ExlnFunction find(int argCount) {
        if(funcs.containsKey(argCount)) {
            return funcs.get(argCount);
        }
        return null;
    }
    public String toString() {
        return "<func " + name + ">";
    }
}
