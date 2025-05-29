package com.marsreg.search.service.impl;

import com.marsreg.search.model.SearchStatistics;
import com.marsreg.search.model.UserBehaviorStats;
import com.marsreg.search.service.DataVisualizationService;
import com.marsreg.search.service.SearchStatisticsService;
import com.marsreg.search.service.UserBehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataVisualizationServiceImpl implements DataVisualizationService {

    private final SearchStatisticsService searchStatisticsService;
    private final UserBehaviorService userBehaviorService;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String SEARCH_TREND_KEY = "search:trend:";
    private static final String USER_ACTIVITY_KEY = "user:activity:";
    private static final String PERFORMANCE_TREND_KEY = "performance:trend:";
    
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter WEEK_FORMATTER = DateTimeFormatter.ofPattern("yyyy-'W'ww");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public Map<String, Long> getSearchTrend(LocalDateTime startTime, LocalDateTime endTime, TimeInterval interval) {
        String pattern = getKeyPattern(SEARCH_TREND_KEY, interval);
        Set<String> keys = redisTemplate.keys(pattern);
        
        Map<String, Long> trend = new TreeMap<>();
        DateTimeFormatter formatter = getFormatter(interval);
        
        // 初始化时间范围内的所有时间点
        LocalDateTime current = startTime;
        while (!current.isAfter(endTime)) {
            trend.put(current.format(formatter), 0L);
            current = incrementTime(current, interval);
        }
        
        // 填充实际数据
        if (keys != null) {
            for (String key : keys) {
                String timeStr = key.substring(SEARCH_TREND_KEY.length());
                LocalDateTime time = parseTime(timeStr, interval);
                if (!time.isBefore(startTime) && !time.isAfter(endTime)) {
                    String formattedTime = time.format(formatter);
                    Long count = Long.valueOf(redisTemplate.opsForValue().get(key));
                    trend.put(formattedTime, count);
                }
            }
        }
        
        return trend;
    }

    @Override
    public Map<String, Long> getSearchTypeDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        SearchStatistics stats = searchStatisticsService.getStatisticsByTimeRange(startTime, endTime);
        return stats.getSearchTypeDistribution().entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().name(),
                Map.Entry::getValue
            ));
    }

    @Override
    public Map<String, Long> getDocumentTypeDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        SearchStatistics stats = searchStatisticsService.getStatisticsByTimeRange(startTime, endTime);
        return stats.getDocumentTypeDistribution();
    }

    @Override
    public Map<String, Long> getUserActivity(LocalDateTime startTime, LocalDateTime endTime, TimeInterval interval) {
        String pattern = getKeyPattern(USER_ACTIVITY_KEY, interval);
        Set<String> keys = redisTemplate.keys(pattern);
        
        Map<String, Long> activity = new TreeMap<>();
        DateTimeFormatter formatter = getFormatter(interval);
        
        // 初始化时间范围内的所有时间点
        LocalDateTime current = startTime;
        while (!current.isAfter(endTime)) {
            activity.put(current.format(formatter), 0L);
            current = incrementTime(current, interval);
        }
        
        // 填充实际数据
        if (keys != null) {
            for (String key : keys) {
                String timeStr = key.substring(USER_ACTIVITY_KEY.length());
                LocalDateTime time = parseTime(timeStr, interval);
                if (!time.isBefore(startTime) && !time.isAfter(endTime)) {
                    String formattedTime = time.format(formatter);
                    Long count = Long.valueOf(redisTemplate.opsForValue().get(key));
                    activity.put(formattedTime, count);
                }
            }
        }
        
        return activity;
    }

    @Override
    public UserBehaviorStats getUserBehaviorAnalysis(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        return userBehaviorService.getUserBehaviorStatsByTimeRange(userId, startTime, endTime);
    }

    @Override
    public Map<String, Double> getPerformanceTrend(LocalDateTime startTime, LocalDateTime endTime, TimeInterval interval) {
        String pattern = getKeyPattern(PERFORMANCE_TREND_KEY, interval);
        Set<String> keys = redisTemplate.keys(pattern);
        
        Map<String, Double> trend = new TreeMap<>();
        DateTimeFormatter formatter = getFormatter(interval);
        
        // 初始化时间范围内的所有时间点
        LocalDateTime current = startTime;
        while (!current.isAfter(endTime)) {
            trend.put(current.format(formatter), 0.0);
            current = incrementTime(current, interval);
        }
        
        // 填充实际数据
        if (keys != null) {
            for (String key : keys) {
                String timeStr = key.substring(PERFORMANCE_TREND_KEY.length());
                LocalDateTime time = parseTime(timeStr, interval);
                if (!time.isBefore(startTime) && !time.isAfter(endTime)) {
                    String formattedTime = time.format(formatter);
                    Double value = Double.valueOf(redisTemplate.opsForValue().get(key));
                    trend.put(formattedTime, value);
                }
            }
        }
        
        return trend;
    }

    @Override
    public List<WordCloudData> getHotKeywordsWordCloud(int size) {
        List<SearchStatistics.KeywordStats> keywords = searchStatisticsService.getHotKeywords(size);
        return keywords.stream()
            .map(keyword -> new WordCloudData(keyword.getKeyword(), keyword.getSearchCount().intValue()))
            .collect(Collectors.toList());
    }
    
    private String getKeyPattern(String prefix, TimeInterval interval) {
        return prefix + "*";
    }
    
    private DateTimeFormatter getFormatter(TimeInterval interval) {
        switch (interval) {
            case HOUR:
                return HOUR_FORMATTER;
            case DAY:
                return DAY_FORMATTER;
            case WEEK:
                return WEEK_FORMATTER;
            case MONTH:
                return MONTH_FORMATTER;
            default:
                return DAY_FORMATTER;
        }
    }
    
    private LocalDateTime incrementTime(LocalDateTime time, TimeInterval interval) {
        switch (interval) {
            case HOUR:
                return time.plusHours(1);
            case DAY:
                return time.plusDays(1);
            case WEEK:
                return time.plusWeeks(1);
            case MONTH:
                return time.plusMonths(1);
            default:
                return time.plusDays(1);
        }
    }
    
    private LocalDateTime parseTime(String timeStr, TimeInterval interval) {
        DateTimeFormatter formatter = getFormatter(interval);
        return LocalDateTime.parse(timeStr, formatter);
    }
} 