package mn.english.platform.user;

import mn.english.platform.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException() {
        super(HttpStatus.UNAUTHORIZED, "Account not found");
    }
}
