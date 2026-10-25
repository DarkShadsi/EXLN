package exln;

public class BoundMethods implements ExlnOverloaded {
    private final ExlnInstance instance;
    private final ExlnClass startClass;
    private final String methodName;

    BoundMethods(ExlnInstance instance, ExlnClass startClass, String methodName) {
        this.instance = instance;
        this.startClass = startClass;
        this.methodName = methodName;
    }

    @Override
    public String name() {
        return methodName;
    }

    @Override
    public ExlnCallable find(int argCount) {
        ExlnFunction fn = startClass.findMethod(methodName, argCount);
        if ( fn == null) return null;
        return fn.bind(instance);
    }

    @Override
    public String toString() {
        return "<method " + methodName + ">";
    }
}