package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SearchStatistics {
    private Long totalSearches;
    private Long uniqueUsers;
    private Double averageResponseTime;
    private Map<SearchType, Long> searchTypeDistribution;
    private Map<String, Long> documentTypeDistribution;
    private List<KeywordStats> topKeywords;
    private TimeRangeStats timeRangeStats;
    
    @Data
    @Builder
    public static class KeywordStats {
        private String keyword;
        private Long searchCount;
        private Long clickCount;
        private Double averageResponseTime;
    }

    @Data
    @Builder
    public static class TimeRangeStats {
        private java.time.LocalDateTime startTime;
        private java.time.LocalDateTime endTime;
        private long totalSearches;
        private Map<String, Long> hourlyDistribution;
        private Map<String, Long> dailyDistribution;
    }
} 