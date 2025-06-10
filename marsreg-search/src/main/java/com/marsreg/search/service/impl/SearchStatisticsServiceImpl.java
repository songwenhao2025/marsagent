package com.marsreg.search.service.impl;

import com.marsreg.search.model.SearchStatistics;
import com.marsreg.search.model.UserBehaviorStats;
import com.marsreg.search.model.SearchType;
import com.marsreg.search.service.SearchStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchStatisticsServiceImpl implements SearchStatisticsService {

    private final RedisTemplate<String, String> redisTemplate;
    
    // 系统级统计键
    private static final String OVERALL_SEARCH_COUNT_KEY = "stats:overall:count";
    private static final String OVERALL_RESPONSE_TIME_KEY = "stats:overall:response_time";
    private static final String OVERALL_SEARCH_TYPE_KEY = "stats:overall:type:";
    private static final String OVERALL_DOC_TYPE_KEY = "stats:overall:doc_type:";
    private static final String OVERALL_KEYWORD_KEY = "stats:overall:keyword";
    
    // 用户级统计键
    private static final String USER_SEARCH_COUNT_KEY = "stats:user:count:";
    private static final String USER_RESPONSE_TIME_KEY = "stats:user:response_time:";
    private static final String USER_SEARCH_TYPE_KEY = "stats:user:type:";
    private static final String USER_DOC_TYPE_KEY = "stats:user:doc_type:";
    private static final String USER_ACTIVE_TIME_KEY = "stats:user:active_time:";
    private static final String USER_KEYWORD_KEY = "stats:user:keyword:";
    private static final String USER_RECENT_SEARCH_KEY = "stats:user:recent:";
    
    private final Map<String, Double> performanceMetrics = new ConcurrentHashMap<>();
    private final DateTimeFormatter hourlyFormatter = DateTimeFormatter.ofPattern("HH");
    private final DateTimeFormatter dailyFormatter = DateTimeFormatter.ofPattern("EEEE");
    private final int MAX_RECENT_SEARCHES = 100;

    @Override
    public SearchStatistics getOverallStatistics() {
        return getStatisticsByTimeRange(null, null);
    }

    @Override
    public SearchStatistics getStatisticsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        // 获取总搜索次数
        Long searchCount = redisTemplate.opsForValue().increment(OVERALL_SEARCH_COUNT_KEY, 0);
        
        // 获取平均响应时间
        Double avgResponseTime = redisTemplate.opsForValue().get(OVERALL_RESPONSE_TIME_KEY) != null ?
            Double.parseDouble(redisTemplate.opsForValue().get(OVERALL_RESPONSE_TIME_KEY)) : 0.0;
        
        // 获取搜索类型分布
        Map<SearchType, Long> searchTypeDistribution = new EnumMap<>(SearchType.class);
        for (SearchType type : SearchType.values()) {
            Long count = redisTemplate.opsForValue().increment(OVERALL_SEARCH_TYPE_KEY + type.name(), 0);
            searchTypeDistribution.put(type, count);
        }
        
        // 获取文档类型分布
        Map<String, Long> documentTypeDistribution = new HashMap<>();
        Set<String> docTypeKeys = redisTemplate.keys(OVERALL_DOC_TYPE_KEY + "*");
        if (docTypeKeys != null) {
            docTypeKeys.forEach(key -> {
                String docType = key.substring(OVERALL_DOC_TYPE_KEY.length());
                Long count = redisTemplate.opsForValue().increment(key, 0);
                documentTypeDistribution.put(docType, count);
            });
        }
        
        // 获取时间范围统计
        SearchStatistics.TimeRangeStats timeRangeStats = getTimeRangeStats(startTime, endTime);
        
        // 获取热门关键词
        List<SearchStatistics.KeywordStats> hotKeywords = getHotKeywords(10);
        
        return SearchStatistics.builder()
            .totalSearches(searchCount != null ? searchCount : 0)
            .averageResponseTime(avgResponseTime)
            .searchTypeDistribution(searchTypeDistribution)
            .documentTypeDistribution(documentTypeDistribution)
            .timeRangeStats(timeRangeStats)
            .topKeywords(hotKeywords)
            .build();
    }

    @Override
    public UserBehaviorStats getUserBehaviorStats(String userId) {
        return getUserBehaviorStatsByTimeRange(userId, null, null);
    }

    @Override
    public UserBehaviorStats getUserBehaviorStatsByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        // 获取用户搜索次数
        Long searchCount = redisTemplate.opsForValue().increment(USER_SEARCH_COUNT_KEY + userId, 0);
        
        // 获取平均响应时间
        Double avgResponseTime = redisTemplate.opsForValue().get(USER_RESPONSE_TIME_KEY + userId) != null ?
            Double.parseDouble(redisTemplate.opsForValue().get(USER_RESPONSE_TIME_KEY + userId)) : 0.0;
        
        // 获取搜索类型分布
        Map<SearchType, Long> searchTypeDistribution = new EnumMap<>(SearchType.class);
        for (SearchType type : SearchType.values()) {
            Long count = redisTemplate.opsForValue().increment(USER_SEARCH_TYPE_KEY + userId + ":" + type.name(), 0);
            searchTypeDistribution.put(type, count);
        }
        
        // 获取文档类型偏好
        Map<String, Long> documentTypePreference = new HashMap<>();
        Set<String> docTypeKeys = redisTemplate.keys(USER_DOC_TYPE_KEY + userId + ":*");
        if (docTypeKeys != null) {
            docTypeKeys.forEach(key -> {
                String docType = key.substring((USER_DOC_TYPE_KEY + userId + ":").length());
                Long count = redisTemplate.opsForValue().increment(key, 0);
                documentTypePreference.put(docType, count);
            });
        }
        
        // 获取活跃时间段分布
        Map<String, Long> activeTimeDistribution = getUserActiveTimeDistribution(userId);
        
        // 获取常用搜索关键词
        List<UserBehaviorStats.KeywordStats> frequentKeywords = getUserFrequentKeywords(userId, 10);
        
        // 获取最近搜索记录
        List<UserBehaviorStats.RecentSearch> recentSearches = getUserRecentSearches(userId, 10);
        
        return UserBehaviorStats.builder()
            .userId(userId)
            .searchCount(searchCount != null ? searchCount : 0)
            .averageResponseTime(avgResponseTime)
            .searchTypeDistribution(searchTypeDistribution)
            .documentTypePreference(documentTypePreference)
            .activeTimeDistribution(activeTimeDistribution)
            .frequentKeywords(frequentKeywords)
            .recentSearches(recentSearches)
            .build();
    }

    @Override
    public void recordSearch(SearchType searchType, String query, long responseTime, String userId, 
                           List<String> documentTypes, double averageScore) {
        // 记录系统级统计
        redisTemplate.opsForValue().increment(OVERALL_SEARCH_COUNT_KEY);
        
        // 更新系统平均响应时间
        String currentAvgTime = redisTemplate.opsForValue().get(OVERALL_RESPONSE_TIME_KEY);
        double newAvgTime = currentAvgTime != null ?
            (Double.parseDouble(currentAvgTime) + responseTime) / 2 : responseTime;
        redisTemplate.opsForValue().set(OVERALL_RESPONSE_TIME_KEY, String.valueOf(newAvgTime));
        
        // 记录系统搜索类型
        redisTemplate.opsForValue().increment(OVERALL_SEARCH_TYPE_KEY + searchType.name());
        
        // 记录系统文档类型
        if (documentTypes != null) {
            documentTypes.forEach(type -> 
                redisTemplate.opsForValue().increment(OVERALL_DOC_TYPE_KEY + type));
        }
        
        // 记录系统关键词
        if (query != null && !query.trim().isEmpty()) {
            redisTemplate.opsForZSet().incrementScore(OVERALL_KEYWORD_KEY, query.trim(), 1.0);
        }
        
        // 记录用户级统计
        if (userId != null) {
            // 记录用户搜索次数
            redisTemplate.opsForValue().increment(USER_SEARCH_COUNT_KEY + userId);
            
            // 更新用户平均响应时间
            String userCurrentAvgTime = redisTemplate.opsForValue().get(USER_RESPONSE_TIME_KEY + userId);
            double userNewAvgTime = userCurrentAvgTime != null ?
                (Double.parseDouble(userCurrentAvgTime) + responseTime) / 2 : responseTime;
            redisTemplate.opsForValue().set(USER_RESPONSE_TIME_KEY + userId, String.valueOf(userNewAvgTime));
            
            // 记录用户搜索类型
            redisTemplate.opsForValue().increment(USER_SEARCH_TYPE_KEY + userId + ":" + searchType.name());
            
            // 记录用户文档类型
            if (documentTypes != null) {
                documentTypes.forEach(type -> 
                    redisTemplate.opsForValue().increment(USER_DOC_TYPE_KEY + userId + ":" + type));
            }
            
            // 记录用户活跃时间
            LocalDateTime now = LocalDateTime.now();
            String hourlyKey = USER_ACTIVE_TIME_KEY + userId + ":hourly:" + now.format(hourlyFormatter);
            String dailyKey = USER_ACTIVE_TIME_KEY + userId + ":daily:" + now.format(dailyFormatter);
            redisTemplate.opsForValue().increment(hourlyKey);
            redisTemplate.opsForValue().increment(dailyKey);
            
            // 记录用户关键词
            if (query != null && !query.trim().isEmpty()) {
                String keywordKey = USER_KEYWORD_KEY + userId;
                redisTemplate.opsForZSet().incrementScore(keywordKey, query.trim(), 1.0);
            }
            
            // 记录用户最近搜索
            String recentSearchKey = USER_RECENT_SEARCH_KEY + userId;
            String searchRecord = String.format("%s|%s|%s|%d|%s|%.2f",
                query, searchType.name(), now.toString(), responseTime,
                String.join(",", documentTypes), averageScore);
            redisTemplate.opsForList().leftPush(recentSearchKey, searchRecord);
            redisTemplate.opsForList().trim(recentSearchKey, 0, MAX_RECENT_SEARCHES - 1);
        }
    }

    @Override
    public List<SearchStatistics.KeywordStats> getHotKeywords(int size) {
        Set<ZSetOperations.TypedTuple<String>> keywords = redisTemplate.opsForZSet()
            .reverseRangeWithScores(OVERALL_KEYWORD_KEY, 0, size - 1);
            
        if (keywords == null) {
            return Collections.emptyList();
        }
        
        return keywords.stream()
            .map(tuple -> SearchStatistics.KeywordStats.builder()
                .keyword(tuple.getValue())
                .searchCount(tuple.getScore().longValue())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public List<UserBehaviorStats.KeywordStats> getUserFrequentKeywords(String userId, int size) {
        Set<ZSetOperations.TypedTuple<String>> keywords = redisTemplate.opsForZSet()
            .reverseRangeWithScores(USER_KEYWORD_KEY + userId, 0, size - 1);
            
        if (keywords == null) {
            return Collections.emptyList();
        }
        
        return keywords.stream()
            .map(tuple -> UserBehaviorStats.KeywordStats.builder()
                .keyword(tuple.getValue())
                .searchCount(tuple.getScore().longValue())
                .averageScore(0.0) // 需要从搜索结果中计算
                .relatedTags(new ArrayList<>()) // 需要从搜索结果中提取
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getUserActiveTimeDistribution(String userId) {
        Map<String, Long> distribution = new TreeMap<>();
        
        // 获取小时分布
        for (int hour = 0; hour < 24; hour++) {
            String key = USER_ACTIVE_TIME_KEY + userId + ":hourly:" + String.format("%02d", hour);
            Long count = redisTemplate.opsForValue().increment(key, 0);
            if (count != null && count > 0) {
                distribution.put(String.format("%02d:00", hour), count);
            }
        }
        
        // 获取星期分布
        String[] weekdays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (String day : weekdays) {
            String key = USER_ACTIVE_TIME_KEY + userId + ":daily:" + day;
            Long count = redisTemplate.opsForValue().increment(key, 0);
            if (count != null && count > 0) {
                distribution.put(day, count);
            }
        }
        
        return distribution;
    }

    @Override
    public List<UserBehaviorStats.RecentSearch> getUserRecentSearches(String userId, int size) {
        List<String> recentSearches = redisTemplate.opsForList()
            .range(USER_RECENT_SEARCH_KEY + userId, 0, size - 1);
            
        if (recentSearches == null) {
            return Collections.emptyList();
        }
        
        return recentSearches.stream()
            .<UserBehaviorStats.RecentSearch>map(this::parseRecentSearch)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private UserBehaviorStats.RecentSearch parseRecentSearch(String record) {
        try {
            String[] parts = record.split("\\|");
            return UserBehaviorStats.RecentSearch.builder()
                .query(parts[0])
                .searchType(SearchType.valueOf(parts[1]))
                .searchTime(LocalDateTime.parse(parts[2]))
                .responseTime(Long.parseLong(parts[3]))
                .documentTypes(Arrays.asList(parts[4].split(",")))
                .averageScore(Double.parseDouble(parts[5]))
                .build();
        } catch (Exception e) {
            log.error("Failed to parse recent search record: {}", record, e);
            return null;
        }
    }

    @Override
    public Map<String, Double> getPerformanceMetrics() {
        Map<String, Double> metrics = new HashMap<>();
        
        // 获取平均响应时间
        String avgResponseTime = redisTemplate.opsForValue().get(OVERALL_RESPONSE_TIME_KEY);
        if (avgResponseTime != null) {
            metrics.put("averageResponseTime", Double.parseDouble(avgResponseTime));
        }
        
        // 获取搜索成功率
        Long totalSearches = redisTemplate.opsForValue().increment(OVERALL_SEARCH_COUNT_KEY, 0);
        Long successfulSearches = redisTemplate.opsForValue().increment(OVERALL_SEARCH_TYPE_KEY + SearchType.SUCCESSFUL.name(), 0);
        if (totalSearches != null && totalSearches > 0 && successfulSearches != null) {
            metrics.put("successRate", (double) successfulSearches / totalSearches);
        }
        
        return metrics;
    }
    
    private SearchStatistics.TimeRangeStats getTimeRangeStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Long> hourlyDistribution = new TreeMap<>();
        Map<String, Long> dailyDistribution = new TreeMap<>();
        
        LocalDateTime current = startTime != null ? startTime : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        
        while (!current.isAfter(end)) {
            String hourlyKey = "search:stats:hourly:" + current.format(hourlyFormatter);
            String dailyKey = "search:stats:daily:" + current.format(dailyFormatter);
            
            Long hourlyCount = redisTemplate.opsForValue().increment(hourlyKey, 0);
            Long dailyCount = redisTemplate.opsForValue().increment(dailyKey, 0);
            
            if (hourlyCount != null && hourlyCount > 0) {
                hourlyDistribution.put(current.format(hourlyFormatter), hourlyCount);
            }
            if (dailyCount != null && dailyCount > 0) {
                dailyDistribution.put(current.format(dailyFormatter), dailyCount);
            }
            
            current = current.plusHours(1);
        }
        
        return SearchStatistics.TimeRangeStats.builder()
            .startTime(startTime)
            .endTime(endTime)
            .totalSearches(hourlyDistribution.values().stream().mapToLong(Long::longValue).sum())
            .hourlyDistribution(hourlyDistribution)
            .dailyDistribution(dailyDistribution)
            .build();
    }
    
    private void updatePerformanceMetrics(long responseTime) {
        performanceMetrics.compute("avg_response_time", (k, v) -> 
            v == null ? responseTime : (v + responseTime) / 2);
        performanceMetrics.compute("max_response_time", (k, v) -> 
            v == null ? responseTime : Math.max(v, responseTime));
        performanceMetrics.compute("min_response_time", (k, v) -> 
            v == null ? responseTime : Math.min(v, responseTime));
    }
} 