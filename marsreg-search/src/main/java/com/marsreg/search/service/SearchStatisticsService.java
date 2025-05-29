package com.marsreg.search.service;

import com.marsreg.search.model.SearchStatistics;
import com.marsreg.search.model.UserBehaviorStats;
import com.marsreg.search.model.SearchType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SearchStatisticsService {
    /**
     * 获取总体搜索统计信息
     *
     * @return 搜索统计信息
     */
    SearchStatistics getOverallStatistics();
    
    /**
     * 获取指定时间范围的搜索统计信息
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 搜索统计信息
     */
    SearchStatistics getStatisticsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取用户行为统计信息
     *
     * @param userId 用户ID
     * @return 用户行为统计信息
     */
    UserBehaviorStats getUserBehaviorStats(String userId);
    
    /**
     * 获取指定时间范围的用户行为统计信息
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户行为统计信息
     */
    UserBehaviorStats getUserBehaviorStatsByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 记录搜索操作
     *
     * @param searchType 搜索类型
     * @param query 搜索关键词
     * @param responseTime 响应时间（毫秒）
     * @param userId 用户ID
     * @param documentTypes 文档类型列表
     * @param averageScore 平均得分
     */
    void recordSearch(SearchType searchType, String query, long responseTime, String userId, 
                     List<String> documentTypes, double averageScore);
    
    /**
     * 获取热门搜索关键词
     *
     * @param size 返回结果数量
     * @return 热门搜索关键词统计
     */
    List<SearchStatistics.KeywordStats> getHotKeywords(int size);
    
    /**
     * 获取用户常用搜索关键词
     *
     * @param userId 用户ID
     * @param size 返回结果数量
     * @return 常用搜索关键词统计
     */
    List<UserBehaviorStats.KeywordStats> getUserFrequentKeywords(String userId, int size);
    
    /**
     * 获取用户活跃时间段分布
     *
     * @param userId 用户ID
     * @return 活跃时间段分布
     */
    Map<String, Long> getUserActiveTimeDistribution(String userId);
    
    /**
     * 获取用户最近搜索记录
     *
     * @param userId 用户ID
     * @param size 返回结果数量
     * @return 最近搜索记录
     */
    List<UserBehaviorStats.RecentSearch> getUserRecentSearches(String userId, int size);
    
    /**
     * 获取搜索性能指标
     *
     * @return 性能指标统计
     */
    Map<String, Double> getPerformanceMetrics();
} 