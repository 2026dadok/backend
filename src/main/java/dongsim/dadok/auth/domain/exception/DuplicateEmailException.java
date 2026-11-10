package dongsim.dadok.auth.domain.exception;

public class DuplicateEmailException extends BaseAuthException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
