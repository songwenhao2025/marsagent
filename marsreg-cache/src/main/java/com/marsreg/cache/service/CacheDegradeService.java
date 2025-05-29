package com.marsreg.cache.service;

import java.util.Map;

/**
 * 缓存降级服务接口
 */
public interface CacheDegradeService {
    
    /**
     * 检查是否允许降级
     * @param key 降级键
     * @return 是否允许降级
     */
    boolean isDegradeAllowed(String key);
    
    /**
     * 获取降级配置
     * @param key 降级键
     * @return 降级配置
     */
    DegradeConfig getConfig(String key);
    
    /**
     * 更新降级配置
     * @param key 降级键
     * @param config 降级配置
     */
    void updateConfig(String key, DegradeConfig config);
    
    /**
     * 重置降级配置
     * @param key 降级键
     */
    void resetConfig(String key);
    
    /**
     * 获取降级统计信息
     * @param key 降级键
     * @return 降级统计信息
     */
    Map<String, Object> getStats(String key);
    
    /**
     * 降级配置类
     */
    class DegradeConfig {
        private double errorRateThreshold; // 错误率阈值
        private int minRequestCount; // 最小请求数
        private int timeWindow; // 时间窗口（秒）
        private boolean enabled; // 是否启用降级
        
        public DegradeConfig() {
            this.errorRateThreshold = 50.0; // 默认50%错误率
            this.minRequestCount = 20; // 最少20个请求
            this.timeWindow = 10; // 10秒时间窗口
            this.enabled = true;
        }
        
        public DegradeConfig(double errorRateThreshold, int minRequestCount, int timeWindow, boolean enabled) {
            this.errorRateThreshold = errorRateThreshold;
            this.minRequestCount = minRequestCount;
            this.timeWindow = timeWindow;
            this.enabled = enabled;
        }
        
        public double getErrorRateThreshold() {
            return errorRateThreshold;
        }
        
        public void setErrorRateThreshold(double errorRateThreshold) {
            this.errorRateThreshold = errorRateThreshold;
        }
        
        public int getMinRequestCount() {
            return minRequestCount;
        }
        
        public void setMinRequestCount(int minRequestCount) {
            this.minRequestCount = minRequestCount;
        }
        
        public int getTimeWindow() {
            return timeWindow;
        }
        
        public void setTimeWindow(int timeWindow) {
            this.timeWindow = timeWindow;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
} 