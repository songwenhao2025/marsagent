package com.marsreg.cache.service;

import java.util.List;
import java.util.Map;

/**
 * 缓存预热服务接口
 */
public interface CacheWarmupService {
    
    /**
     * 按类型预热缓存
     * @param type 缓存类型
     * @return 预热结果
     */
    WarmupResult warmupByType(String type);
    
    /**
     * 预热所有缓存
     * @return 预热结果
     */
    WarmupResult warmupAll();
    
    /**
     * 按模式预热缓存
     * @param pattern 键模式
     * @return 预热结果
     */
    WarmupResult warmupByPattern(String pattern);
    
    /**
     * 从数据源预热缓存
     * @param type 缓存类型
     * @param data 数据源
     * @return 预热结果
     */
    WarmupResult warmupFromData(String type, Map<String, Object> data);
    
    /**
     * 从键列表预热缓存
     * @param type 缓存类型
     * @param keys 键列表
     * @return 预热结果
     */
    WarmupResult warmupFromKeys(String type, List<String> keys);
    
    /**
     * 获取预热状态
     * @param type 缓存类型
     * @return 预热状态
     */
    WarmupStatus getWarmupStatus(String type);
    
    /**
     * 获取总体预热状态
     * @return 预热状态
     */
    WarmupStatus getTotalWarmupStatus();
    
    /**
     * 预热结果类
     */
    class WarmupResult {
        private int successCount;
        private int failureCount;
        private long startTime;
        private long endTime;
        private String type;
        private String pattern;
        
        public WarmupResult() {
            this.startTime = System.currentTimeMillis();
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }
        
        public int getFailureCount() {
            return failureCount;
        }
        
        public void setFailureCount(int failureCount) {
            this.failureCount = failureCount;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }
        
        public long getEndTime() {
            return endTime;
        }
        
        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        
        public long getDuration() {
            return endTime - startTime;
        }
        
        public double getSuccessRate() {
            int total = successCount + failureCount;
            return total > 0 ? (double) successCount / total * 100 : 0;
        }
    }
    
    /**
     * 预热状态接口
     */
    interface WarmupStatus {
        int getSuccessCount();
        int getFailureCount();
        long getStartTime();
        long getEndTime();
        long getDuration();
        double getSuccessRate();
    }
} 