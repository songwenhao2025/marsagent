package com.marsreg.search.exception;

import lombok.Getter;

@Getter
public class SearchException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;

    public SearchException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public SearchException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
} 