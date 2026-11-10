package mn.english.platform.common.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Base class for expected errors that we want to return to the client.
 * Each feature (auth, user, ...) can extend it without depending on other features.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Extra HTTP headers for the response. Most errors have none. */
    public HttpHeaders headers() {
        return new HttpHeaders();
    }
}
