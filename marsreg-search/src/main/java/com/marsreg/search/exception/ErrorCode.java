package com.marsreg.search.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INVALID_REQUEST("SEARCH_001", "无效的请求参数"),
    SEARCH_FAILED("SEARCH_002", "搜索失败"),
    VECTOR_SEARCH_NOT_IMPLEMENTED("SEARCH_003", "向量搜索功能尚未实现"),
    HYBRID_SEARCH_NOT_IMPLEMENTED("SEARCH_004", "混合搜索功能尚未实现"),
    INTERNAL_ERROR("SEARCH_999", "服务器内部错误");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
} 