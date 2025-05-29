package com.marsreg.search.service.impl;

import com.marsreg.search.model.UserBehaviorStats;
import com.marsreg.search.model.UserBehaviorStats.RecentSearch;
import com.marsreg.search.model.SearchType;
import com.marsreg.search.service.UserBehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorServiceImpl implements UserBehaviorService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String USER_SEARCH_COUNT_KEY = "user:stats:count:";
    private static final String USER_RESPONSE_TIME_KEY = "user:stats:response_time:";
    private static final String USER_SEARCH_TYPE_KEY = "user:stats:type:";
    private static final String USER_DOC_TYPE_KEY = "user:stats:doc_type:";
    private static final String USER_ACTIVE_TIME_KEY = "user:stats:active_time:";
    private static final String USER_KEYWORD_KEY = "user:stats:keyword:";
    private static final String USER_RECENT_SEARCH_KEY = "user:stats:recent:";
    
    private final DateTimeFormatter hourlyFormatter = DateTimeFormatter.ofPattern("HH");
    private final DateTimeFormatter dailyFormatter = DateTimeFormatter.ofPattern("EEEE");
    private final int MAX_RECENT_SEARCHES = 100;

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
        List<RecentSearch> recentSearches = getUserRecentSearches(userId, 10);
        
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
    public void recordUserSearch(String userId, SearchType searchType, String query, long responseTime, 
                               List<String> documentTypes, double averageScore) {
        // 记录搜索次数
        redisTemplate.opsForValue().increment(USER_SEARCH_COUNT_KEY + userId);
        
        // 更新平均响应时间
        String currentAvgTime = redisTemplate.opsForValue().get(USER_RESPONSE_TIME_KEY + userId);
        double newAvgTime = currentAvgTime != null ?
            (Double.parseDouble(currentAvgTime) + responseTime) / 2 : responseTime;
        redisTemplate.opsForValue().set(USER_RESPONSE_TIME_KEY + userId, String.valueOf(newAvgTime));
        
        // 记录搜索类型
        redisTemplate.opsForValue().increment(USER_SEARCH_TYPE_KEY + userId + ":" + searchType.name());
        
        // 记录文档类型
        if (documentTypes != null) {
            documentTypes.forEach(type -> 
                redisTemplate.opsForValue().increment(USER_DOC_TYPE_KEY + userId + ":" + type));
        }
        
        // 记录活跃时间
        LocalDateTime now = LocalDateTime.now();
        String hourlyKey = USER_ACTIVE_TIME_KEY + userId + ":hourly:" + now.format(hourlyFormatter);
        String dailyKey = USER_ACTIVE_TIME_KEY + userId + ":daily:" + now.format(dailyFormatter);
        redisTemplate.opsForValue().increment(hourlyKey);
        redisTemplate.opsForValue().increment(dailyKey);
        
        // 记录关键词
        if (query != null && !query.trim().isEmpty()) {
            String keywordKey = USER_KEYWORD_KEY + userId;
            redisTemplate.opsForZSet().incrementScore(keywordKey, query.trim(), 1.0);
        }
        
        // 记录最近搜索
        String recentSearchKey = USER_RECENT_SEARCH_KEY + userId;
        String searchRecord = String.format("%s|%s|%s|%d|%s|%.2f",
            query, searchType.name(), now.toString(), responseTime,
            String.join(",", documentTypes), averageScore);
        redisTemplate.opsForList().leftPush(recentSearchKey, searchRecord);
        redisTemplate.opsForList().trim(recentSearchKey, 0, MAX_RECENT_SEARCHES - 1);
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
    public List<RecentSearch> getUserRecentSearches(String userId, int size) {
        List<String> recentSearches = redisTemplate.opsForList()
            .range(USER_RECENT_SEARCH_KEY + userId, 0, size - 1);
            
        if (recentSearches == null) {
            return Collections.emptyList();
        }
        
        return recentSearches.stream()
            .<RecentSearch>map(this::parseRecentSearch)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private RecentSearch parseRecentSearch(String record) {
        try {
            String[] parts = record.split("\\|");
            return RecentSearch.builder()
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
} 