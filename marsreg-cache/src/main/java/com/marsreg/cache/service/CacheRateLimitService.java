package com.marsreg.cache.service;

import java.util.Map;

/**
 * 缓存限流服务接口
 */
public interface CacheRateLimitService {
    
    /**
     * 尝试获取令牌
     * @param key 限流键
     * @return 是否获取成功
     */
    boolean tryAcquire(String key);
    
    /**
     * 尝试获取多个令牌
     * @param key 限流键
     * @param permits 令牌数量
     * @return 是否获取成功
     */
    boolean tryAcquire(String key, int permits);
    
    /**
     * 获取限流配置
     * @param key 限流键
     * @return 限流配置
     */
    RateLimitConfig getConfig(String key);
    
    /**
     * 更新限流配置
     * @param key 限流键
     * @param config 限流配置
     */
    void updateConfig(String key, RateLimitConfig config);
    
    /**
     * 重置限流配置
     * @param key 限流键
     */
    void resetConfig(String key);
    
    /**
     * 获取限流统计信息
     * @param key 限流键
     * @return 限流统计信息
     */
    Map<String, Object> getStats(String key);
    
    /**
     * 限流配置类
     */
    class RateLimitConfig {
        private int permitsPerSecond; // 每秒允许的请求数
        private int burstSize; // 突发流量大小
        private boolean enabled; // 是否启用限流
        
        public RateLimitConfig() {
            this.permitsPerSecond = 100;
            this.burstSize = 200;
            this.enabled = true;
        }
        
        public RateLimitConfig(int permitsPerSecond, int burstSize, boolean enabled) {
            this.permitsPerSecond = permitsPerSecond;
            this.burstSize = burstSize;
            this.enabled = enabled;
        }
        
        public int getPermitsPerSecond() {
            return permitsPerSecond;
        }
        
        public void setPermitsPerSecond(int permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
        }
        
        public int getBurstSize() {
            return burstSize;
        }
        
        public void setBurstSize(int burstSize) {
            this.burstSize = burstSize;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
} 