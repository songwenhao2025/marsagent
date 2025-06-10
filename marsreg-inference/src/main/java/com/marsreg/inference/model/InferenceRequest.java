package com.marsreg.inference.model;

import lombok.Data;
import lombok.Builder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class InferenceRequest {
    @NotBlank(message = "问题不能为空")
    private String question;
    
    @NotNull(message = "搜索类型不能为空")
    private SearchType searchType;
    
    private Integer maxDocuments;
    private Float minSimilarity;
    private List<String> documentTypes;
    private Map<String, Object> parameters;
    
    public enum SearchType {
        VECTOR,
        KEYWORD,
        HYBRID
    }
} 