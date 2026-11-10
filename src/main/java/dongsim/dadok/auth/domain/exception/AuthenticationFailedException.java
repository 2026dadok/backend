package dongsim.dadok.auth.domain.exception;

public class AuthenticationFailedException extends BaseAuthException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
