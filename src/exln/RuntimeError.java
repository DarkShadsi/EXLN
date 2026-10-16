package exln;

public class RuntimeError extends RuntimeException{
    Token token;
    String message;
    public RuntimeError(Token token, String message) {
        this.token = token;
        this.message = message;
    }
}
