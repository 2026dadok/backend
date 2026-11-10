package dongsim.dadok.auth.domain.exception;

public class UserNotFoundException extends BaseAuthException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
