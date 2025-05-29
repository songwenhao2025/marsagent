package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class UserBehaviorStats {
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 搜索次数
     */
    private long searchCount;
    
    /**
     * 平均响应时间（毫秒）
     */
    private double averageResponseTime;
    
    /**
     * 搜索类型分布
     */
    private Map<SearchType, Long> searchTypeDistribution;
    
    /**
     * 常用搜索关键词
     */
    private List<KeywordStats> frequentKeywords;
    
    /**
     * 文档类型偏好
     */
    private Map<String, Long> documentTypePreference;
    
    /**
     * 活跃时间段分布
     */
    private Map<String, Long> activeTimeDistribution;
    
    /**
     * 最近搜索记录
     */
    private List<RecentSearch> recentSearches;
    
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
    public static class RecentSearch {
        private String query;
        private SearchType searchType;
        private LocalDateTime searchTime;
        private long responseTime;
        private List<String> documentTypes;
        private double averageScore;
    }
} 