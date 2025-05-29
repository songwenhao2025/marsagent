package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;

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
    private SuggestionType type;
    
    /**
     * 相关度分数
     */
    private float score;
    
    /**
     * 额外信息（如文档ID、标签等）
     */
    private String extraInfo;
    
    public enum SuggestionType {
        KEYWORD,
        DOCUMENT,
        TAG
    }
} 