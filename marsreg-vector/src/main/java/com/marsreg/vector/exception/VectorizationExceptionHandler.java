package com.marsreg.vector.exception;

import com.marsreg.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class VectorizationExceptionHandler {
    
    @ExceptionHandler(VectorizationException.class)
    public ApiResponse<Void> handleVectorizationException(VectorizationException e) {
        log.error("向量化异常", e);
        return ApiResponse.error(e.getMessage());
    }
} 