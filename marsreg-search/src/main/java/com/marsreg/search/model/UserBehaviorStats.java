package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class UserBehaviorStats {
    private String userId;
    private Long searchCount;
    private Double averageResponseTime;
    private Map<SearchType, Long> searchTypeDistribution;
    private Map<String, Long> documentTypePreference;
    private List<KeywordStats> frequentKeywords;
    private Map<String, Long> activeTimeDistribution;
    private List<RecentSearch> recentSearches;
    
    @Data
    @Builder
    public static class KeywordStats {
        private String keyword;
        private Long searchCount;
        private Double averageScore;
        private List<String> relatedTags;
    }
    
    @Data
    @Builder
    public static class RecentSearch {
        private String query;
        private SearchType searchType;
        private java.time.LocalDateTime searchTime;
        private Long responseTime;
        private List<String> documentTypes;
        private Double averageScore;
    }
} 