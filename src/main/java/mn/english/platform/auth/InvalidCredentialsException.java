package mn.english.platform.auth;

import mn.english.platform.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        // One message for "unknown email" and "wrong password" so attackers learn nothing.
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
