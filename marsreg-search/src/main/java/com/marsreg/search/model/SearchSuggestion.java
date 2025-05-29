package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

@Data
@Builder
public class SearchSuggestion {
    /**
     * 建议文本
     */
    private String text;
    
    /**
     * 建议类型：KEYWORD-关键词, DOCUMENT-文档标题, TAG-标签
     */
    private String type;
    
    /**
     * 相关度分数
     */
    private Double score;
    
    /**
     * 分类
     */
    private String category;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    
    public enum SuggestionType {
        KEYWORD,
        DOCUMENT,
        TAG
    }
} 