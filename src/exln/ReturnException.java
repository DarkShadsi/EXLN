package exln;

public class ReturnException extends RuntimeException{
    Object value;
    ReturnException(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
