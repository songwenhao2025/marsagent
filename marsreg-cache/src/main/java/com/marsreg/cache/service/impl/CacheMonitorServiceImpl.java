package com.marsreg.cache.service.impl;

import com.marsreg.cache.config.CacheConfig;
import com.marsreg.cache.service.CacheMonitorService;
import com.marsreg.cache.service.CacheStatsService;
import com.marsreg.cache.service.MultiLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存监控服务实现
 */
@Slf4j
@Service
public class CacheMonitorServiceImpl implements CacheMonitorService {

    @Autowired
    private MultiLevelCacheService cacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheConfig cacheConfig;

    @Autowired
    private CacheStatsService cacheStatsService;

    private final Map<String, Object> monitorConfig = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> alerts = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> healthStatus = new HashMap<>();
        
        // 检查Redis连接状态
        boolean redisConnected = checkRedisConnection();
        healthStatus.put("redisConnected", redisConnected);
        
        // 检查本地缓存状态
        boolean localCacheEnabled = cacheConfig.getLocal().isEnabled();
        healthStatus.put("localCacheEnabled", localCacheEnabled);
        
        // 检查分布式缓存状态
        boolean distributedCacheEnabled = cacheConfig.getDistributed().isEnabled();
        healthStatus.put("distributedCacheEnabled", distributedCacheEnabled);
        
        // 检查多级缓存状态
        boolean multiLevelCacheEnabled = cacheConfig.getMultiLevel().isEnabled();
        healthStatus.put("multiLevelCacheEnabled", multiLevelCacheEnabled);
        
        // 总体健康状态
        boolean overallHealth = redisConnected && 
                              (localCacheEnabled || distributedCacheEnabled || multiLevelCacheEnabled);
        healthStatus.put("overallHealth", overallHealth);
        
        return healthStatus;
    }

    @Override
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // 获取缓存统计信息
        CacheStatsService.CacheStats totalStats = cacheStatsService.getTotalStats();
        metrics.put("hitRate", totalStats.getHitRate());
        metrics.put("averageLoadTime", totalStats.getAverageLoadTime());
        metrics.put("loadSuccessRate", totalStats.getLoadSuccessRate());
        
        // 获取各类型缓存的性能指标
        Map<String, Double> hitRateStats = cacheStatsService.getHitRateStats();
        metrics.put("hitRateByType", hitRateStats);
        
        Map<String, Long> loadTimeStats = cacheStatsService.getLoadTimeStats();
        metrics.put("loadTimeByType", loadTimeStats);
        
        return metrics;
    }

    @Override
    public Map<String, Object> getCapacityUsage() {
        Map<String, Object> usage = new HashMap<>();
        
        // 获取各类型缓存的大小
        Map<String, Long> sizeStats = cacheStatsService.getSizeStats();
        usage.put("sizeByType", sizeStats);
        
        // 计算总容量使用率
        long totalSize = sizeStats.values().stream().mapToLong(Long::longValue).sum();
        long maxSize = cacheConfig.getLocal().getMaximumSize() + 
                      cacheConfig.getDistributed().getMaximumSize();
        double usageRate = maxSize > 0 ? (double) totalSize / maxSize * 100 : 0;
        usage.put("totalUsageRate", usageRate);
        
        return usage;
    }

    @Override
    public Map<String, Object> getErrorStats() {
        Map<String, Object> errorStats = new HashMap<>();
        
        // 获取各类型缓存的错误统计
        Map<String, Long> errorCountsByType = new HashMap<>();
        errorCounts.forEach((type, count) -> errorCountsByType.put(type, count.get()));
        errorStats.put("errorCountsByType", errorCountsByType);
        
        // 计算总错误数
        long totalErrors = errorCountsByType.values().stream().mapToLong(Long::longValue).sum();
        errorStats.put("totalErrors", totalErrors);
        
        return errorStats;
    }

    @Override
    public Map<String, Object> getAlerts() {
        Map<String, Object> alertInfo = new HashMap<>();
        alertInfo.put("alerts", alerts);
        alertInfo.put("alertCount", alerts.size());
        return alertInfo;
    }

    @Override
    public Map<String, Object> getMonitorConfig() {
        return new HashMap<>(monitorConfig);
    }

    @Override
    public void updateMonitorConfig(Map<String, Object> config) {
        monitorConfig.clear();
        monitorConfig.putAll(config);
        log.info("更新缓存监控配置: {}", config);
    }

    @Override
    public void resetMonitorData() {
        errorCounts.clear();
        alerts.clear();
        log.info("重置缓存监控数据");
    }

    @Override
    public Long getCacheExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key);
            return expire != null ? expire : -1L;
        } catch (Exception e) {
            log.error("获取缓存过期时间失败, key: {}", key, e);
            return -1L;
        }
    }

    @Override
    public Long getCacheSize(String type) {
        try {
            if (type == null || type.isEmpty()) {
                return 0L;
            }
            Map<String, Long> sizeStats = cacheStatsService.getSizeStats();
            switch (type.toLowerCase()) {
                case "local":
                    return sizeStats.getOrDefault("local", 0L);
                case "distributed":
                    Set<String> keys = redisTemplate.keys("*");
                    return keys != null ? (long) keys.size() : 0L;
                case "multilevel":
                    long local = sizeStats.getOrDefault("local", 0L);
                    Set<String> dkeys = redisTemplate.keys("*");
                    long distributed = dkeys != null ? dkeys.size() : 0L;
                    return local + distributed;
                default:
                    return 0L;
            }
        } catch (Exception e) {
            log.error("获取缓存大小失败, type: {}", type, e);
            return 0L;
        }
    }

    @Override
    public Map<String, Long> getCacheMemoryUsage() {
        Map<String, Long> memoryUsage = new HashMap<>();
        
        // 获取本地缓存内存使用情况
        if (cacheConfig.getLocal().isEnabled()) {
            long localMemoryUsage = getLocalCacheMemoryUsage();
            memoryUsage.put("local", localMemoryUsage);
        }
        
        // 获取分布式缓存内存使用情况
        if (cacheConfig.getDistributed().isEnabled()) {
            long distributedMemoryUsage = getDistributedCacheMemoryUsage();
            memoryUsage.put("distributed", distributedMemoryUsage);
        }
        
        // 获取多级缓存内存使用情况
        if (cacheConfig.getMultiLevel().isEnabled()) {
            long multiLevelMemoryUsage = getMultiLevelCacheMemoryUsage();
            memoryUsage.put("multiLevel", multiLevelMemoryUsage);
        }
        
        return memoryUsage;
    }
    
    private long getLocalCacheMemoryUsage() {
        try {
            // 使用JVM内存管理API获取本地缓存内存使用情况
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            return usedMemory;
        } catch (Exception e) {
            log.error("获取本地缓存内存使用情况失败", e);
            return 0L;
        }
    }
    
    private long getDistributedCacheMemoryUsage() {
        try {
            // 使用Redis INFO命令获取内存使用情况
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            String usedMemory = info.getProperty("used_memory");
            return usedMemory != null ? Long.parseLong(usedMemory) : 0L;
        } catch (Exception e) {
            log.error("获取分布式缓存内存使用情况失败", e);
            return 0L;
        }
    }
    
    private long getMultiLevelCacheMemoryUsage() {
        // 多级缓存内存使用情况为本地缓存和分布式缓存的总和
        return getLocalCacheMemoryUsage() + getDistributedCacheMemoryUsage();
    }

    private boolean checkRedisConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.error("Redis连接检查失败", e);
            return false;
        }
    }

    public void recordError(String type, String error) {
        errorCounts.computeIfAbsent(type, k -> new AtomicLong(0)).incrementAndGet();
        
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", type);
        alert.put("error", error);
        alert.put("timestamp", new Date());
        alerts.add(alert);
        
        log.error("缓存错误 - 类型: {}, 错误: {}", type, error);
    }

    @Override
    public Double getCacheHitRate(String type) {
        CacheStatsService.CacheStats stats = cacheStatsService.getStats(type);
        return stats.getHitRate();
    }

    @Override
    public Object getCacheValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取缓存值失败, key: {}", key, e);
            return null;
        }
    }

    @Override
    public List<String> getCacheKeys(String type) {
        try {
            Set<String> keys = redisTemplate.keys(type + ":*");
            return keys != null ? new ArrayList<>(keys) : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取缓存键列表失败, type: {}", type, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getCacheTypes() {
        List<String> types = new ArrayList<>();
        if (cacheConfig.getLocal().isEnabled()) {
            types.add("local");
        }
        if (cacheConfig.getDistributed().isEnabled()) {
            types.add("distributed");
        }
        if (cacheConfig.getMultiLevel().isEnabled()) {
            types.add("multilevel");
        }
        return types;
    }

    @Override
    public CacheMonitorService.CacheStats getCacheStatsByKey(String key) {
        try {
            CacheStatsService.CacheStats stats = cacheStatsService.getStats(key);
            CacheMonitorService.CacheStats result = new CacheMonitorService.CacheStats();
            result.setHitCount(stats.getHitCount());
            result.setMissCount(stats.getMissCount());
            result.setTotalSize(stats.getTotalLoadTime());
            result.setLastAccessTime(System.currentTimeMillis());
            result.setLastUpdateTime(System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("获取缓存统计信息失败, key: {}", key, e);
            return new CacheMonitorService.CacheStats();
        }
    }

    @Override
    public CacheMonitorService.CacheStats getCacheStatsByType(String type) {
        try {
            CacheStatsService.CacheStats stats = cacheStatsService.getStats(type);
            CacheMonitorService.CacheStats result = new CacheMonitorService.CacheStats();
            result.setHitCount(stats.getHitCount());
            result.setMissCount(stats.getMissCount());
            result.setTotalSize(stats.getTotalLoadTime());
            result.setLastAccessTime(System.currentTimeMillis());
            result.setLastUpdateTime(System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("获取缓存统计信息失败, type: {}", type, e);
            return new CacheMonitorService.CacheStats();
        }
    }

    @Override
    public CacheMonitorService.CacheStats getCacheStats() {
        try {
            CacheStatsService.CacheStats stats = cacheStatsService.getTotalStats();
            CacheMonitorService.CacheStats result = new CacheMonitorService.CacheStats();
            result.setHitCount(stats.getHitCount());
            result.setMissCount(stats.getMissCount());
            result.setTotalSize(stats.getTotalLoadTime());
            result.setLastAccessTime(System.currentTimeMillis());
            result.setLastUpdateTime(System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("获取缓存统计信息失败", e);
            return new CacheMonitorService.CacheStats();
        }
    }
} 