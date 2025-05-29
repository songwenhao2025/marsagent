package com.marsreg.inference.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 系统错误
    INTERNAL_ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),
    
    // 请求错误
    INVALID_REQUEST(400, "无效的请求参数"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    
    // 模型错误
    MODEL_NOT_FOUND(1001, "模型不存在"),
    MODEL_LOADING_FAILED(1002, "模型加载失败"),
    MODEL_INFERENCE_FAILED(1003, "模型推理失败"),
    MODEL_ACTIVATION_FAILED(1004, "模型激活失败"),
    MODEL_DEACTIVATION_FAILED(1005, "模型停用失败"),
    
    // 上下文错误
    CONTEXT_EXTRACTION_FAILED(2001, "上下文提取失败"),
    CONTEXT_TOO_LARGE(2002, "上下文过大"),
    CONTEXT_EMPTY(2003, "上下文为空"),
    
    // 流式处理错误
    STREAM_INITIALIZATION_FAILED(3001, "流式处理初始化失败"),
    STREAM_PROCESSING_FAILED(3002, "流式处理失败"),
    STREAM_TIMEOUT(3003, "流式处理超时"),
    
    // 缓存错误
    CACHE_OPERATION_FAILED(4001, "缓存操作失败"),
    CACHE_KEY_NOT_FOUND(4002, "缓存键不存在"),
    
    // 监控错误
    METRICS_COLLECTION_FAILED(5001, "指标收集失败"),
    METRICS_EXPORT_FAILED(5002, "指标导出失败");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
} 