package com.marsreg.cache.service.impl;

import com.marsreg.cache.service.CacheRateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存限流服务实现
 */
@Slf4j
@Service
public class CacheRateLimitServiceImpl implements CacheRateLimitService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final Map<String, RateLimitConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> rejectCounts = new ConcurrentHashMap<>();

    private static final String RATE_LIMIT_SCRIPT = 
        "local key = KEYS[1] " +
        "local permits = tonumber(ARGV[1]) " +
        "local now = tonumber(ARGV[2]) " +
        "local window = tonumber(ARGV[3]) " +
        "local limit = tonumber(ARGV[4]) " +
        "local burst = tonumber(ARGV[5]) " +
        "local current = redis.call('get', key) " +
        "if current == false then " +
        "    redis.call('set', key, permits, 'EX', window) " +
        "    return 1 " +
        "end " +
        "if tonumber(current) + permits <= limit + burst then " +
        "    redis.call('incrby', key, permits) " +
        "    return 1 " +
        "end " +
        "return 0";

    private final DefaultRedisScript<Long> rateLimitScript = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);

    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1);
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        RateLimitConfig config = getConfig(key);
        if (!config.isEnabled()) {
            return true;
        }

        requestCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();

        List<String> keys = Collections.singletonList(getRateLimitKey(key));
        long now = System.currentTimeMillis() / 1000;
        Object[] args = {
            permits,
            now,
            1, // 时间窗口（秒）
            config.getPermitsPerSecond(),
            config.getBurstSize()
        };

        Long result = redisTemplate.execute(rateLimitScript, keys, args);
        boolean acquired = result != null && result == 1;

        if (!acquired) {
            rejectCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
            log.debug("限流请求被拒绝 - 键: {}, 令牌数: {}", key, permits);
        }

        return acquired;
    }

    @Override
    public RateLimitConfig getConfig(String key) {
        return configs.computeIfAbsent(key, k -> new RateLimitConfig());
    }

    @Override
    public void updateConfig(String key, RateLimitConfig config) {
        configs.put(key, config);
        log.info("更新限流配置 - 键: {}, 配置: {}", key, config);
    }

    @Override
    public void resetConfig(String key) {
        configs.remove(key);
        requestCounts.remove(key);
        rejectCounts.remove(key);
        log.info("重置限流配置 - 键: {}", key);
    }

    @Override
    public Map<String, Object> getStats(String key) {
        Map<String, Object> stats = new HashMap<>();
        
        RateLimitConfig config = getConfig(key);
        stats.put("config", config);
        
        AtomicLong requestCount = requestCounts.get(key);
        stats.put("requestCount", requestCount != null ? requestCount.get() : 0);
        
        AtomicLong rejectCount = rejectCounts.get(key);
        stats.put("rejectCount", rejectCount != null ? rejectCount.get() : 0);
        
        if (requestCount != null && requestCount.get() > 0) {
            double rejectRate = (double) (rejectCount != null ? rejectCount.get() : 0) / requestCount.get() * 100;
            stats.put("rejectRate", rejectRate);
        }
        
        return stats;
    }

    private String getRateLimitKey(String key) {
        return "rate_limit:" + key;
    }
} 