package com.marsreg.search.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SearchResponse {
    private List<SearchResult> results;
    private long totalHits;
    private long tookMs;
    private boolean timedOut;
    
    @Data
    public static class SearchResult {
        private String id;
        private String title;
        private String content;
        private double score;
        private Map<String, String[]> highlights;
        private Map<String, Object> metadata;
    }
} 