package com.marsreg.common.response;

import lombok.Data;

@Data
public class Result<T> {
    private boolean success;
    private String message;
    private T data;
    private String code;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        result.setCode("200");
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setMessage(message);
        result.setCode("500");
        return result;
    }
} 