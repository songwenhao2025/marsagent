package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SearchResult {
    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 相似度分数
     */
    private Float score;

    /**
     * 元数据
     */
    private String metadata;

    /**
     * 高亮片段
     */
    private String highlight;
} 