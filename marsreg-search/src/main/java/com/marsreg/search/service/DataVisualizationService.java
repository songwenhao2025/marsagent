package com.marsreg.search.service;

import com.marsreg.search.model.SearchStatistics;
import com.marsreg.search.model.UserBehaviorStats;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DataVisualizationService {
    /**
     * 获取搜索趋势数据
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param interval 时间间隔（HOUR/DAY/WEEK/MONTH）
     * @return 时间序列数据
     */
    Map<String, Long> getSearchTrend(LocalDateTime startTime, LocalDateTime endTime, TimeInterval interval);
    
    /**
     * 获取搜索类型分布数据
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 搜索类型分布数据
     */
    Map<String, Long> getSearchTypeDistribution(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取文档类型分布数据
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 文档类型分布数据
     */
    Map<String, Long> getDocumentTypeDistribution(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取用户活跃度数据
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param interval 时间间隔（HOUR/DAY/WEEK/MONTH）
     * @return 用户活跃度数据
     */
    Map<String, Long> getUserActivity(LocalDateTime startTime, LocalDateTime endTime, TimeInterval interval);
    
    /**
     * 获取用户行为分析数据
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户行为分析数据
     */
    UserBehaviorStats getUserBehaviorAnalysis(String userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取性能指标趋势数据
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param interval 时间间隔（HOUR/DAY/WEEK/MONTH）
     * @return 性能指标趋势数据
     */
    Map<String, Double> getPerformanceTrend(LocalDateTime startTime, LocalDateTime endTime, TimeInterval interval);
    
    /**
     * 获取热门关键词词云数据
     *
     * @param size 关键词数量
     * @return 词云数据
     */
    List<WordCloudData> getHotKeywordsWordCloud(int size);
    
    enum TimeInterval {
        HOUR,
        DAY,
        WEEK,
        MONTH
    }
    
    class WordCloudData {
        private String text;
        private int value;
        
        public WordCloudData(String text, int value) {
            this.text = text;
            this.value = value;
        }
        
        public String getText() {
            return text;
        }
        
        public int getValue() {
            return value;
        }
    }
} 