package com.marsreg.inference.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice("com.marsreg.inference")
public class InferenceGlobalExceptionHandler {

    @Data
    public static class ErrorResponse {
        private final LocalDateTime timestamp;
        private final String errorCode;
        private final String message;
        private final String path;

        public ErrorResponse(String errorCode, String message, String path) {
            this.timestamp = LocalDateTime.now();
            this.errorCode = errorCode;
            this.message = message;
            this.path = path;
        }
    }

    @ExceptionHandler(InferenceException.class)
    public ResponseEntity<ErrorResponse> handleInferenceException(InferenceException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getErrorMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            String.valueOf(ErrorCode.INTERNAL_ERROR.getCode()),
            ErrorCode.INTERNAL_ERROR.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 