package com.marsreg.cache.service;

import java.util.List;
import java.util.Map;

/**
 * 缓存监控服务接口
 */
public interface CacheMonitorService {
    
    /**
     * 获取缓存统计信息
     * @return 统计信息
     */
    CacheStats getCacheStats();
    
    /**
     * 获取缓存类型统计信息
     * @param type 缓存类型
     * @return 统计信息
     */
    CacheStats getCacheStatsByType(String type);
    
    /**
     * 获取缓存键统计信息
     * @param key 缓存键
     * @return 统计信息
     */
    CacheStats getCacheStatsByKey(String key);
    
    /**
     * 获取缓存类型列表
     * @return 类型列表
     */
    List<String> getCacheTypes();
    
    /**
     * 获取缓存键列表
     * @param type 缓存类型
     * @return 键列表
     */
    List<String> getCacheKeys(String type);
    
    /**
     * 获取缓存值
     * @param key 缓存键
     * @return 缓存值
     */
    Object getCacheValue(String key);
    
    /**
     * 获取缓存过期时间
     * @param key 缓存键
     * @return 过期时间（秒）
     */
    Long getCacheExpire(String key);
    
    /**
     * 获取缓存大小
     * @param type 缓存类型
     * @return 缓存大小
     */
    Long getCacheSize(String type);
    
    /**
     * 获取缓存命中率
     * @param type 缓存类型
     * @return 命中率
     */
    Double getCacheHitRate(String type);
    
    /**
     * 获取缓存内存使用情况
     * @return 内存使用情况
     */
    Map<String, Long> getCacheMemoryUsage();
    
    /**
     * 获取缓存健康状态
     * @return 健康状态信息
     */
    Map<String, Object> getHealthStatus();
    
    /**
     * 获取缓存性能指标
     * @return 性能指标信息
     */
    Map<String, Object> getPerformanceMetrics();
    
    /**
     * 获取缓存容量使用情况
     * @return 容量使用信息
     */
    Map<String, Object> getCapacityUsage();
    
    /**
     * 获取缓存错误统计
     * @return 错误统计信息
     */
    Map<String, Object> getErrorStats();
    
    /**
     * 获取缓存告警信息
     * @return 告警信息列表
     */
    Map<String, Object> getAlerts();
    
    /**
     * 获取缓存监控配置
     * @return 监控配置信息
     */
    Map<String, Object> getMonitorConfig();
    
    /**
     * 更新缓存监控配置
     * @param config 监控配置信息
     */
    void updateMonitorConfig(Map<String, Object> config);
    
    /**
     * 重置缓存监控数据
     */
    void resetMonitorData();
    
    /**
     * 缓存统计信息类
     */
    class CacheStats {
        private long totalKeys;
        private long totalSize;
        private long hitCount;
        private long missCount;
        private long expireCount;
        private long evictCount;
        private long lastAccessTime;
        private long lastUpdateTime;
        
        public long getTotalKeys() {
            return totalKeys;
        }
        
        public void setTotalKeys(long totalKeys) {
            this.totalKeys = totalKeys;
        }
        
        public long getTotalSize() {
            return totalSize;
        }
        
        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }
        
        public long getHitCount() {
            return hitCount;
        }
        
        public void setHitCount(long hitCount) {
            this.hitCount = hitCount;
        }
        
        public long getMissCount() {
            return missCount;
        }
        
        public void setMissCount(long missCount) {
            this.missCount = missCount;
        }
        
        public long getExpireCount() {
            return expireCount;
        }
        
        public void setExpireCount(long expireCount) {
            this.expireCount = expireCount;
        }
        
        public long getEvictCount() {
            return evictCount;
        }
        
        public void setEvictCount(long evictCount) {
            this.evictCount = evictCount;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public void setLastAccessTime(long lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
        }
        
        public long getLastUpdateTime() {
            return lastUpdateTime;
        }
        
        public void setLastUpdateTime(long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }
        
        public double getHitRate() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total * 100 : 0;
        }
        
        public double getMissRate() {
            long total = hitCount + missCount;
            return total > 0 ? (double) missCount / total * 100 : 0;
        }
    }
} 