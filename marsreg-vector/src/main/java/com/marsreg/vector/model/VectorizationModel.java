package com.marsreg.vector.model;

import java.util.List;

public interface VectorizationModel {
    
    /**
     * 获取模型名称
     */
    String getModelName();
    
    /**
     * 获取模型版本
     */
    String getModelVersion();
    
    /**
     * 获取向量维度
     */
    int getDimension();
    
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
     * 预热模型
     */
    void warmup();
    
    /**
     * 更新模型
     * @param modelPath 模型路径
     */
    void update(String modelPath);
} 