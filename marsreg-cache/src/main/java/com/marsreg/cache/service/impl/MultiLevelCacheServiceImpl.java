package com.marsreg.cache.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.marsreg.cache.service.MultiLevelCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiLevelCacheServiceImpl implements MultiLevelCacheService {

    private final CacheManager localCacheManager;
    private final CacheManager redisCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cache.local.max-size}")
    private long localMaxSize;

    @Value("${cache.local.expire-seconds}")
    private long localExpireSeconds;

    @Value("${cache.distributed.expire-seconds}")
    private long distributedExpireSeconds;

    private final Cache<String, Object> localCache = Caffeine.newBuilder()
        .maximumSize(localMaxSize)
        .expireAfterWrite(localExpireSeconds, TimeUnit.SECONDS)
        .recordStats()
        .build();

    @Override
    public <T> T get(String key, Class<T> type) {
        // 1. 尝试从本地缓存获取
        T value = getFromLocalCache(key, type);
        if (value != null) {
            return value;
        }

        // 2. 尝试从分布式缓存获取
        value = getFromDistributedCache(key, type);
        if (value != null) {
            // 更新本地缓存
            putToLocalCache(key, value);
            return value;
        }

        return null;
    }

    @Override
    public <T> T get(String key, Class<T> type, Callable<T> valueLoader) {
        // 1. 尝试从本地缓存获取
        T value = getFromLocalCache(key, type);
        if (value != null) {
            return value;
        }

        // 2. 尝试从分布式缓存获取
        value = getFromDistributedCache(key, type);
        if (value != null) {
            // 更新本地缓存
            putToLocalCache(key, value);
            return value;
        }

        // 3. 加载新值
        try {
            value = valueLoader.call();
            if (value != null) {
                // 更新两级缓存
                putToLocalCache(key, value);
                putToDistributedCache(key, value);
            }
            return value;
        } catch (Exception e) {
            log.error("Failed to load value for key: " + key, e);
            return null;
        }
    }

    @Override
    public void put(String key, Object value) {
        putToLocalCache(key, value);
        putToDistributedCache(key, value);
    }

    @Override
    public void evict(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(key);
    }

    @Override
    public void clear() {
        localCache.invalidateAll();
        // 注意：这里不清除Redis缓存，因为可能影响其他实例
    }

    @Override
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = localCache.stats();
        return new CacheStats() {
            @Override
            public long getHitCount() {
                return stats.hitCount();
            }

            @Override
            public long getMissCount() {
                return stats.missCount();
            }

            @Override
            public long getLoadSuccessCount() {
                return stats.loadSuccessCount();
            }

            @Override
            public long getLoadFailureCount() {
                return stats.loadFailureCount();
            }

            @Override
            public long getTotalLoadTime() {
                return stats.totalLoadTime();
            }

            @Override
            public long getEvictionCount() {
                return stats.evictionCount();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T getFromLocalCache(String key, Class<T> type) {
        try {
            return (T) localCache.getIfPresent(key);
        } catch (Exception e) {
            log.error("Failed to get value from local cache for key: " + key, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getFromDistributedCache(String key, Class<T> type) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get value from distributed cache for key: " + key, e);
            return null;
        }
    }

    private void putToLocalCache(String key, Object value) {
        try {
            localCache.put(key, value);
        } catch (Exception e) {
            log.error("Failed to put value to local cache for key: " + key, e);
        }
    }

    private void putToDistributedCache(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value, distributedExpireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to put value to distributed cache for key: " + key, e);
        }
    }
} 