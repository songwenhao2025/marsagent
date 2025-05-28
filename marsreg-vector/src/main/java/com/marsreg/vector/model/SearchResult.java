package com.marsreg.vector.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SearchResult {
    /**
     * 文档ID
     */
    private String id;

    /**
     * 相似度分数
     */
    private float score;

    /**
     * 向量数据
     */
    private float[] vector;

    /**
     * 元数据
     */
    private Object metadata;
} 