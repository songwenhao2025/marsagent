package com.marsreg.vector.service;

import java.util.List;

public interface VectorizationService {
    /**
     * 将文本转换为向量
     * @param text 输入文本
     * @return 向量数组
     */
    float[] vectorize(String text);

    /**
     * 批量将文本转换为向量
     * @param texts 输入文本列表
     * @return 向量数组列表
     */
    List<float[]> batchVectorize(List<String> texts);

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