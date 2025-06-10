package com.marsreg.vector.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 句子向量化服务接口
 */
public interface SentenceVectorizationService extends VectorizationService {
    /**
     * 获取向量维度
     * @return 向量维度
     */
    int getDimension();

    /**
     * 异步批量向量化
     * @param texts 文本列表
     * @return 向量列表的CompletableFuture
     */
    CompletableFuture<List<float[]>> batchVectorizeAsync(List<String> texts);
} 