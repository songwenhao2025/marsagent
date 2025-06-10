package com.marsreg.document.exception;

/**
 * 文档处理异常
 */
public class DocumentProcessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public DocumentProcessException(String message) {
        super(message);
    }
    
    public DocumentProcessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DocumentProcessException(Throwable cause) {
        super(cause);
    }
} 