package searchengine.exceptions;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final int HttpStatus;

    public ApiException(String message, int HttpStatus) {
        super(message);
        this.HttpStatus = HttpStatus;
    }
}