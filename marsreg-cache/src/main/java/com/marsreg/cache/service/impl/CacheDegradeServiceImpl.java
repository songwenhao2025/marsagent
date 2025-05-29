package com.marsreg.cache.service.impl;

import com.marsreg.cache.service.CacheDegradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存降级服务实现
 */
@Slf4j
@Service
public class CacheDegradeServiceImpl implements CacheDegradeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final Map<String, DegradeConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();

    private static final String DEGRADE_SCRIPT = 
        "local key = KEYS[1] " +
        "local now = tonumber(ARGV[1]) " +
        "local window = tonumber(ARGV[2]) " +
        "local minCount = tonumber(ARGV[3]) " +
        "local errorThreshold = tonumber(ARGV[4]) " +
        "local requestCount = redis.call('get', key .. ':requests') " +
        "local errorCount = redis.call('get', key .. ':errors') " +
        "if requestCount == false or errorCount == false then " +
        "    return 0 " +
        "end " +
        "requestCount = tonumber(requestCount) " +
        "errorCount = tonumber(errorCount) " +
        "if requestCount < minCount then " +
        "    return 0 " +
        "end " +
        "local errorRate = errorCount / requestCount * 100 " +
        "if errorRate >= errorThreshold then " +
        "    return 1 " +
        "end " +
        "return 0";

    private final DefaultRedisScript<Long> degradeScript = new DefaultRedisScript<>(DEGRADE_SCRIPT, Long.class);

    @Override
    public boolean isDegradeAllowed(String key) {
        DegradeConfig config = getConfig(key);
        if (!config.isEnabled()) {
            return false;
        }

        List<String> keys = Collections.singletonList(getDegradeKey(key));
        long now = System.currentTimeMillis() / 1000;
        Object[] args = {
            now,
            config.getTimeWindow(),
            config.getMinRequestCount(),
            config.getErrorRateThreshold()
        };

        Long result = redisTemplate.execute(degradeScript, keys, args);
        return result != null && result == 1;
    }

    @Override
    public DegradeConfig getConfig(String key) {
        return configs.computeIfAbsent(key, k -> new DegradeConfig());
    }

    @Override
    public void updateConfig(String key, DegradeConfig config) {
        configs.put(key, config);
        log.info("更新降级配置 - 键: {}, 配置: {}", key, config);
    }

    @Override
    public void resetConfig(String key) {
        configs.remove(key);
        requestCounts.remove(key);
        errorCounts.remove(key);
        redisTemplate.delete(getDegradeKey(key) + ":requests");
        redisTemplate.delete(getDegradeKey(key) + ":errors");
        log.info("重置降级配置 - 键: {}", key);
    }

    @Override
    public Map<String, Object> getStats(String key) {
        Map<String, Object> stats = new HashMap<>();
        
        DegradeConfig config = getConfig(key);
        stats.put("config", config);
        
        AtomicLong requestCount = requestCounts.get(key);
        stats.put("requestCount", requestCount != null ? requestCount.get() : 0);
        
        AtomicLong errorCount = errorCounts.get(key);
        stats.put("errorCount", errorCount != null ? errorCount.get() : 0);
        
        if (requestCount != null && requestCount.get() > 0) {
            double errorRate = (double) (errorCount != null ? errorCount.get() : 0) / requestCount.get() * 100;
            stats.put("errorRate", errorRate);
        }
        
        return stats;
    }

    private String getDegradeKey(String key) {
        return "degrade:" + key;
    }
} 