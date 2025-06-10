package com.marsreg.vector.service;

import java.util.List;
import java.util.Map;

public interface VectorStorageService {
    /**
     * 存储向量
     * @param id 向量ID
     * @param vector 向量数据
     */
    void storeVector(String id, float[] vector);

    /**
     * 批量存储向量
     * @param vectors 向量ID和数据的映射
     */
    void storeVectors(Map<String, float[]> vectors);

    /**
     * 获取向量
     * @param id 向量ID
     * @return 向量数据
     */
    float[] getVector(String id);

    /**
     * 批量获取向量
     * @param ids 向量ID列表
     * @return 向量ID和数据的映射
     */
    Map<String, float[]> getVectors(List<String> ids);

    /**
     * 删除向量
     * @param id 向量ID
     */
    void deleteVector(String id);

    /**
     * 批量删除向量
     * @param ids 向量ID列表
     */
    void deleteVectors(List<String> ids);

    /**
     * 搜索相似向量
     * @param queryVector 查询向量
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似向量ID和分数的映射
     */
    Map<String, Float> searchSimilar(float[] queryVector, int limit, float minScore);

    /**
     * 在指定范围内搜索相似向量
     * @param queryVector 查询向量
     * @param vectorIds 向量ID范围
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似向量ID和分数的映射
     */
    Map<String, Float> searchSimilarInRange(float[] queryVector, List<String> vectorIds, int limit, float minScore);
} 