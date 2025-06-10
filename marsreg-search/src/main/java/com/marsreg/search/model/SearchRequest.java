package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class SearchRequest {
    @NotBlank(message = "查询关键词不能为空")
    private String query;
    
    @NotNull(message = "搜索类型不能为空")
    private SearchType searchType;
    
    @Min(value = 0, message = "页码不能小于0")
    private Integer page;
    
    @Min(value = 1, message = "每页大小不能小于1")
    private Integer size;
    
    private List<String> documentTypes;
    private Map<String, Object> filters;
    private List<String> fields;
    private Float minScore;
    private String sortField;
    private String sortOrder;
    private Set<String> aggregations;
    
    public enum SearchType {
        KEYWORD,    // 关键词搜索
        VECTOR,     // 向量搜索
        HYBRID      // 混合搜索
    }
} 