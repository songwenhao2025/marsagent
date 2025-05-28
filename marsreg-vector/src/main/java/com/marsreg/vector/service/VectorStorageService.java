package com.marsreg.vector.service;

import java.util.List;
import java.util.Map;

public interface VectorStorageService {
    /**
     * 存储单个向量
     * @param id 向量ID
     * @param vector 向量数据
     */
    void store(String id, float[] vector);

    /**
     * 批量存储向量
     * @param vectors 向量ID到向量数据的映射
     */
    void batchStore(Map<String, float[]> vectors);

    /**
     * 更新向量
     * @param id 向量ID
     * @param vector 新的向量数据
     */
    void updateVector(String id, float[] vector);

    /**
     * 删除向量
     * @param id 向量ID
     */
    void delete(String id);

    /**
     * 根据前缀删除向量
     * @param prefix 向量ID前缀
     */
    void deleteByPrefix(String prefix);

    /**
     * 搜索相似向量
     * @param queryVector 查询向量
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，每个结果包含向量ID和相似度分数
     */
    List<Map.Entry<String, Float>> search(float[] queryVector, int limit, float minScore);

    /**
     * 在指定前缀范围内搜索相似向量
     * @param queryVector 查询向量
     * @param prefix 向量ID前缀
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，每个结果包含向量ID和相似度分数
     */
    List<Map.Entry<String, Float>> searchByPrefix(float[] queryVector, String prefix, int limit, float minScore);
} 