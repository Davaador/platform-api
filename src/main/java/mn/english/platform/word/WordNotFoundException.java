package mn.english.platform.word;

import mn.english.platform.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class WordNotFoundException extends ApiException {

    public WordNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Word not found");
    }
}
