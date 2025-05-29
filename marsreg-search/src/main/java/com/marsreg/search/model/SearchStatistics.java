package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SearchStatistics {
    /**
     * 总搜索次数
     */
    private long totalSearches;
    
    /**
     * 总搜索用户数
     */
    private long totalUsers;
    
    /**
     * 平均响应时间（毫秒）
     */
    private double averageResponseTime;
    
    /**
     * 搜索类型分布
     */
    private Map<SearchType, Long> searchTypeDistribution;
    
    /**
     * 热门搜索关键词
     */
    private List<KeywordStats> hotKeywords;
    
    /**
     * 文档类型分布
     */
    private Map<String, Long> documentTypeDistribution;
    
    /**
     * 时间范围统计
     */
    private TimeRangeStats timeRangeStats;
    
    @Data
    @Builder
    public static class KeywordStats {
        private String keyword;
        private long searchCount;
        private double averageScore;
        private List<String> relatedTags;
    }
    
    @Data
    @Builder
    public static class TimeRangeStats {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long totalSearches;
        private Map<String, Long> hourlyDistribution;
        private Map<String, Long> dailyDistribution;
    }
} 