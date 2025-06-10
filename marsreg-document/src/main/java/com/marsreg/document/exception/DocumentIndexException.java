package com.marsreg.document.exception;

public class DocumentIndexException extends RuntimeException {
    
    private final String errorCode;
    
    public DocumentIndexException(String message) {
        super(message);
        this.errorCode = "DOC_INDEX_001";
    }
    
    public DocumentIndexException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public DocumentIndexException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DOC_INDEX_001";
    }
    
    public DocumentIndexException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
} 