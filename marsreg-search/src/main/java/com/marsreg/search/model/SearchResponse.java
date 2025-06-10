package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SearchResponse {
    private List<SearchResult> results;
    private long total;
    private int page;
    private int size;
    private Map<String, Object> aggregations;
    private long took;
    
    @Data
    @Builder
    public static class SearchResult {
        private String id;
        private String title;
        private String content;
        private String type;
        private float score;
        private Map<String, Object> metadata;
        private List<String> highlights;
    }
} 