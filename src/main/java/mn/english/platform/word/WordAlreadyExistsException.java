package mn.english.platform.word;

import org.springframework.http.HttpStatus;

import mn.english.platform.common.error.ApiException;

public class WordAlreadyExistsException extends ApiException {
    public WordAlreadyExistsException(){
        super(HttpStatus.CONFLICT, "Word already exists");
    }
}
