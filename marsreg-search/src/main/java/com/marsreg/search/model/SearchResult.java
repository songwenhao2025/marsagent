package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

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
     * 文档类型
     */
    private String documentType;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 高亮标题
     */
    private String highlightedTitle;

    /**
     * 高亮内容片段
     */
    private List<String> highlightedContents;

    /**
     * 摘要
     */
    private String summary;
} 