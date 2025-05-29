package com.marsreg.search.service.impl;

import com.marsreg.search.model.SearchStatistics;
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
    
    private static final String SEARCH_COUNT_KEY = "search:stats:count";
    private static final String USER_COUNT_KEY = "search:stats:users";
    private static final String RESPONSE_TIME_KEY = "search:stats:response_time";
    private static final String SEARCH_TYPE_KEY = "search:stats:type:";
    private static final String DOCUMENT_TYPE_KEY = "search:stats:doc_type:";
    private static final String HOURLY_STATS_KEY = "search:stats:hourly:";
    private static final String DAILY_STATS_KEY = "search:stats:daily:";
    private static final String KEYWORD_STATS_KEY = "search:stats:keyword:";
    
    private final Map<String, Double> performanceMetrics = new ConcurrentHashMap<>();
    private final DateTimeFormatter hourlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH");
    private final DateTimeFormatter dailyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public SearchStatistics getOverallStatistics() {
        return getStatisticsByTimeRange(null, null);
    }

    @Override
    public SearchStatistics getStatisticsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        // 获取总搜索次数
        Long totalSearches = redisTemplate.opsForValue().increment(SEARCH_COUNT_KEY, 0);
        
        // 获取总用户数
        Long totalUsers = redisTemplate.opsForSet().size(USER_COUNT_KEY);
        
        // 获取平均响应时间
        Double avgResponseTime = redisTemplate.opsForValue().get(RESPONSE_TIME_KEY) != null ?
            Double.parseDouble(redisTemplate.opsForValue().get(RESPONSE_TIME_KEY)) : 0.0;
        
        // 获取搜索类型分布
        Map<SearchType, Long> searchTypeDistribution = new EnumMap<>(SearchType.class);
        for (SearchType type : SearchType.values()) {
            Long count = redisTemplate.opsForValue().increment(SEARCH_TYPE_KEY + type.name(), 0);
            searchTypeDistribution.put(type, count);
        }
        
        // 获取文档类型分布
        Map<String, Long> documentTypeDistribution = new HashMap<>();
        Set<String> docTypeKeys = redisTemplate.keys(DOCUMENT_TYPE_KEY + "*");
        if (docTypeKeys != null) {
            docTypeKeys.forEach(key -> {
                String docType = key.substring(DOCUMENT_TYPE_KEY.length());
                Long count = redisTemplate.opsForValue().increment(key, 0);
                documentTypeDistribution.put(docType, count);
            });
        }
        
        // 获取时间范围统计
        SearchStatistics.TimeRangeStats timeRangeStats = getTimeRangeStats(startTime, endTime);
        
        // 获取热门关键词
        List<SearchStatistics.KeywordStats> hotKeywords = getHotKeywords(10);
        
        return SearchStatistics.builder()
            .totalSearches(totalSearches != null ? totalSearches : 0)
            .totalUsers(totalUsers != null ? totalUsers : 0)
            .averageResponseTime(avgResponseTime)
            .searchTypeDistribution(searchTypeDistribution)
            .documentTypeDistribution(documentTypeDistribution)
            .timeRangeStats(timeRangeStats)
            .hotKeywords(hotKeywords)
            .build();
    }

    @Override
    public void recordSearch(SearchType searchType, String query, long responseTime, String userId, List<String> documentTypes) {
        // 记录搜索次数
        redisTemplate.opsForValue().increment(SEARCH_COUNT_KEY);
        
        // 记录用户
        if (userId != null) {
            redisTemplate.opsForSet().add(USER_COUNT_KEY, userId);
        }
        
        // 更新平均响应时间
        String currentAvgTime = redisTemplate.opsForValue().get(RESPONSE_TIME_KEY);
        double newAvgTime = currentAvgTime != null ?
            (Double.parseDouble(currentAvgTime) + responseTime) / 2 : responseTime;
        redisTemplate.opsForValue().set(RESPONSE_TIME_KEY, String.valueOf(newAvgTime));
        
        // 记录搜索类型
        redisTemplate.opsForValue().increment(SEARCH_TYPE_KEY + searchType.name());
        
        // 记录文档类型
        if (documentTypes != null) {
            documentTypes.forEach(type -> 
                redisTemplate.opsForValue().increment(DOCUMENT_TYPE_KEY + type));
        }
        
        // 记录时间分布
        LocalDateTime now = LocalDateTime.now();
        String hourlyKey = HOURLY_STATS_KEY + now.format(hourlyFormatter);
        String dailyKey = DAILY_STATS_KEY + now.format(dailyFormatter);
        redisTemplate.opsForValue().increment(hourlyKey);
        redisTemplate.opsForValue().increment(dailyKey);
        
        // 记录关键词
        if (query != null && !query.trim().isEmpty()) {
            String keywordKey = KEYWORD_STATS_KEY + query.trim();
            redisTemplate.opsForZSet().incrementScore(keywordKey, query.trim(), 1.0);
        }
        
        // 更新性能指标
        updatePerformanceMetrics(responseTime);
    }

    @Override
    public List<SearchStatistics.KeywordStats> getHotKeywords(int size) {
        Set<ZSetOperations.TypedTuple<String>> hotKeywords = redisTemplate.opsForZSet()
            .reverseRangeWithScores(KEYWORD_STATS_KEY, 0, size - 1);
            
        if (hotKeywords == null) {
            return Collections.emptyList();
        }
        
        return hotKeywords.stream()
            .map(tuple -> SearchStatistics.KeywordStats.builder()
                .keyword(tuple.getValue())
                .searchCount(tuple.getScore().longValue())
                .averageScore(0.0) // 需要从搜索结果中计算
                .relatedTags(new ArrayList<>()) // 需要从搜索结果中提取
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Double> getPerformanceMetrics() {
        return new HashMap<>(performanceMetrics);
    }
    
    private SearchStatistics.TimeRangeStats getTimeRangeStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Long> hourlyDistribution = new TreeMap<>();
        Map<String, Long> dailyDistribution = new TreeMap<>();
        
        LocalDateTime current = startTime != null ? startTime : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        
        while (!current.isAfter(end)) {
            String hourlyKey = HOURLY_STATS_KEY + current.format(hourlyFormatter);
            String dailyKey = DAILY_STATS_KEY + current.format(dailyFormatter);
            
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