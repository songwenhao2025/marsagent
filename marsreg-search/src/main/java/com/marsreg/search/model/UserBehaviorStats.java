package com.marsreg.search.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UserBehaviorStats {
    private Long searchCount;
    private Double averageResponseTime;
    private List<KeywordStats> frequentKeywords;
    private Map<String, Long> activeTimeDistribution;
    private List<RecentSearch> recentSearches;
    
    @Data
    public static class KeywordStats {
        private String keyword;
        private Long count;
    }
    
    @Data
    public static class RecentSearch {
        private String keyword;
        private SearchType searchType;
        private Long timestamp;
    }
} 