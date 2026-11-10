package mn.english.platform.auth;

import mn.english.platform.common.error.ApiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class TooManyLoginAttemptsException extends ApiException {

    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Please try again later.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /** Tells the client how many seconds to wait (standard HTTP header). */
    @Override
    public HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        return headers;
    }
}
