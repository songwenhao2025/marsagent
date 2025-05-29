package com.marsreg.inference.service;

import com.marsreg.inference.model.InferenceRequest;
import com.marsreg.inference.model.InferenceResponse;

public interface InferenceService {
    /**
     * 执行推理
     *
     * @param request 推理请求
     * @return 推理响应
     */
    InferenceResponse infer(InferenceRequest request);
    
    /**
     * 执行流式推理
     *
     * @param request 推理请求
     * @param callback 回调函数
     */
    void inferStream(InferenceRequest request, StreamCallback callback);
    
    /**
     * 流式回调接口
     */
    interface StreamCallback {
        void onToken(String token);
        void onComplete();
        void onError(Throwable error);
    }
} 