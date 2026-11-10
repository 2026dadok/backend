package dongsim.dadok.auth.domain.exception;

public abstract class BaseAuthException extends RuntimeException {
    public BaseAuthException(String message) {
        super(message);
    }
}
