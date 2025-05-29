package com.marsreg.inference.service;

import java.util.List;
import java.util.Map;

public interface LLMService {
    /**
     * 生成回答
     *
     * @param prompt 提示词
     * @param context 上下文
     * @param parameters 参数
     * @return 生成的回答
     */
    String generateAnswer(String prompt, List<String> context, Map<String, Object> parameters);
    
    /**
     * 生成回答（流式）
     *
     * @param prompt 提示词
     * @param context 上下文
     * @param parameters 参数
     * @param callback 回调函数
     */
    void generateAnswerStream(String prompt, List<String> context, Map<String, Object> parameters, 
                            StreamCallback callback);
    
    /**
     * 流式回调接口
     */
    interface StreamCallback {
        void onToken(String token);
        void onComplete();
        void onError(Throwable error);
    }
} 