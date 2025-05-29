package com.marsreg.vector.exception;

import com.marsreg.common.exception.BusinessException;

public class VectorizationException extends BusinessException {
    
    public VectorizationException(String message) {
        super(message);
    }
    
    public VectorizationException(String message, Throwable cause) {
        super(message, cause);
    }
} 