package com.marsreg.common.response;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String code;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>()
                .setSuccess(true)
                .setCode("200")
                .setMessage("success")
                .setData(data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<T>()
                .setSuccess(false)
                .setCode("500")
                .setMessage(message);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<T>()
                .setSuccess(false)
                .setCode(code)
                .setMessage(message);
    }
} 