package com.marsreg.cache.service;

import java.util.Map;

/**
 * 缓存统计服务接口
 */
public interface CacheStatsService {
    
    /**
     * 获取指定类型的缓存统计信息
     * @param type 缓存类型
     * @return 缓存统计信息
     */
    CacheStats getStats(String type);
    
    /**
     * 获取所有缓存的统计信息
     * @return 缓存统计信息
     */
    CacheStats getTotalStats();
    
    /**
     * 重置指定类型的缓存统计信息
     * @param type 缓存类型
     */
    void resetStats(String type);
    
    /**
     * 重置所有缓存的统计信息
     */
    void resetAllStats();
    
    /**
     * 获取缓存大小统计
     * @return 各类型缓存的大小统计
     */
    Map<String, Long> getSizeStats();
    
    /**
     * 获取缓存命中率统计
     * @return 各类型缓存的命中率统计
     */
    Map<String, Double> getHitRateStats();
    
    /**
     * 获取缓存加载时间统计
     * @return 各类型缓存的加载时间统计
     */
    Map<String, Long> getLoadTimeStats();
    
    /**
     * 缓存统计信息接口
     */
    interface CacheStats {
        /**
         * 获取命中次数
         */
        long getHitCount();
        
        /**
         * 获取未命中次数
         */
        long getMissCount();
        
        /**
         * 获取加载成功次数
         */
        long getLoadSuccessCount();
        
        /**
         * 获取加载失败次数
         */
        long getLoadFailureCount();
        
        /**
         * 获取总加载时间
         */
        long getTotalLoadTime();
        
        /**
         * 获取驱逐次数
         */
        long getEvictionCount();
        
        /**
         * 获取命中率
         */
        double getHitRate();
        
        /**
         * 获取平均加载时间
         */
        double getAverageLoadTime();
        
        /**
         * 获取加载成功率
         */
        double getLoadSuccessRate();
    }
} 