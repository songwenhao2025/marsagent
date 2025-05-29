package com.marsreg.cache.service;

import java.util.List;
import java.util.Map;

/**
 * 缓存清理服务接口
 */
public interface CacheCleanupService {
    
    /**
     * 按类型清理缓存
     * @param type 缓存类型
     * @return 清理结果
     */
    CleanupResult cleanupByType(String type);
    
    /**
     * 清理所有缓存
     * @return 清理结果
     */
    CleanupResult cleanupAll();
    
    /**
     * 按模式清理缓存
     * @param pattern 键模式
     * @return 清理结果
     */
    CleanupResult cleanupByPattern(String pattern);
    
    /**
     * 按键列表清理缓存
     * @param keys 键列表
     * @return 清理结果
     */
    CleanupResult cleanupByKeys(List<String> keys);
    
    /**
     * 清理过期缓存
     * @return 清理结果
     */
    CleanupResult cleanupExpired();
    
    /**
     * 获取清理状态
     * @param type 缓存类型
     * @return 清理状态
     */
    CleanupStatus getCleanupStatus(String type);
    
    /**
     * 获取总体清理状态
     * @return 清理状态
     */
    CleanupStatus getTotalCleanupStatus();
    
    /**
     * 清理结果类
     */
    class CleanupResult {
        private int successCount;
        private int failureCount;
        private long startTime;
        private long endTime;
        private String type;
        private String pattern;
        
        public CleanupResult() {
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
     * 清理状态接口
     */
    interface CleanupStatus {
        int getSuccessCount();
        int getFailureCount();
        long getStartTime();
        long getEndTime();
        long getDuration();
        double getSuccessRate();
    }
} 