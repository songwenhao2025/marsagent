package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SearchRequest {
    /**
     * 查询文本
     */
    private String query;

    /**
     * 检索类型：VECTOR-向量检索, KEYWORD-关键词检索, HYBRID-混合检索
     */
    private SearchType searchType;

    /**
     * 返回结果数量
     */
    private Integer size;

    /**
     * 最小相似度阈值
     */
    private Float minSimilarity;

    /**
     * 过滤条件
     */
    private String filter;

    // 排序相关字段
    private List<SortField> sortFields;
    
    // 过滤相关字段
    private List<String> documentTypes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> tags;
    private Map<String, Object> customFilters;

    public enum SearchType {
        VECTOR,
        KEYWORD,
        HYBRID
    }

    @Data
    @Builder
    public static class SortField {
        private String field;
        private SortOrder order;
        
        public enum SortOrder {
            ASC, DESC
        }
    }
} 