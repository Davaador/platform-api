package mn.english.platform.user;

import mn.english.platform.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyUsedException extends ApiException {

    public EmailAlreadyUsedException() {
        super(HttpStatus.CONFLICT, "Email is already registered");
    }
}
