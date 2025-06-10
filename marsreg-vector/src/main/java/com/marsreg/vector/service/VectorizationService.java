package com.marsreg.vector.service;

import java.util.List;
import java.util.Map;

public interface VectorizationService {
    /**
     * 将文本转换为向量
     * @param text 输入文本
     * @return 向量表示
     */
    float[] vectorize(String text);

    /**
     * 批量将文本转换为向量
     * @param texts 输入文本列表
     * @return 向量表示列表
     */
    List<float[]> batchVectorize(List<String> texts);

    /**
     * 使用缓存将文本转换为向量
     * @param text 输入文本
     * @return 向量表示
     */
    float[] vectorizeWithCache(String text);

    /**
     * 使用缓存批量将文本转换为向量
     * @param texts 输入文本列表
     * @return 向量表示列表
     */
    List<float[]> batchVectorizeWithCache(List<String> texts);

    /**
     * 计算两个向量的余弦相似度
     * @param vector1 第一个向量
     * @param vector2 第二个向量
     * @return 相似度分数
     */
    float calculateSimilarity(float[] vector1, float[] vector2);

    /**
     * 计算向量之间的相似度矩阵
     * @param vectors 向量列表
     * @return 相似度矩阵
     */
    List<List<Float>> calculateSimilarityMatrix(List<float[]> vectors);

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
     * 向量归一化
     * @param vector 输入向量
     * @return 归一化后的向量
     */
    float[] normalize(float[] vector);
} 