package com.marsreg.vector.service;

import java.util.List;
import java.util.Map;

public interface VectorizationService {
    /**
     * 文本向量化
     * @param text 输入文本
     * @return 向量
     */
    List<Float> vectorize(String text);

    /**
     * 批量文本向量化
     * @param texts 输入文本列表
     * @return 向量列表
     */
    List<List<Float>> batchVectorize(List<String> texts);

    /**
     * 带缓存的文本向量化
     * @param text 输入文本
     * @return 向量
     */
    List<Float> vectorizeWithCache(String text);

    /**
     * 带缓存的批量文本向量化
     * @param texts 输入文本列表
     * @return 向量列表
     */
    List<List<Float>> batchVectorizeWithCache(List<String> texts);

    /**
     * 获取向量化模型信息
     * @return 模型信息
     */
    Map<String, Object> getModelInfo();

    /**
     * 更新向量化模型
     * @param modelPath 模型路径
     */
    void updateModel(String modelPath);

    /**
     * 预热向量化模型
     */
    void warmupModel();

    /**
     * 获取向量维度
     * @return 向量维度
     */
    int getDimension();

    /**
     * 计算向量相似度
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度分数
     */
    float calculateSimilarity(float[] vector1, float[] vector2);

    /**
     * 向量归一化
     * @param vector 输入向量
     * @return 归一化后的向量
     */
    float[] normalize(float[] vector);
} 