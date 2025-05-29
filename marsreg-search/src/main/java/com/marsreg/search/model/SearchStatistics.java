package com.marsreg.search.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SearchStatistics {
    private Long totalSearches;
    private Long uniqueUsers;
    private Double averageResponseTime;
    private Map<SearchType, Long> searchTypeDistribution;
    private Map<String, Long> documentTypeDistribution;
    private List<KeywordStats> topKeywords;
    
    @Data
    public static class KeywordStats {
        private String keyword;
        private Long count;
    }
} 