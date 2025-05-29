package com.marsreg.vector.cache;

import com.marsreg.vector.cache.eviction.CacheEvictionStrategy;
import com.marsreg.vector.cache.eviction.LRUEvictionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorCacheManager {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheEvictionStrategy evictionStrategy = new LRUEvictionStrategy();
    
    // 本地缓存
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private static final int LOCAL_CACHE_SIZE = 1000;
    private static final long LOCAL_CACHE_EXPIRE = 300; // 5分钟
    
    // Redis缓存配置
    private static final String REDIS_CACHE_KEY_PREFIX = "vector:cache:";
    private static final int REDIS_CACHE_EXPIRE = 3600; // 1小时
    private static final int MAX_REDIS_CACHE_SIZE = 10000;
    
    // 缓存统计
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    
    public List<Float> get(String key) {
        // 1. 尝试从本地缓存获取
        CacheEntry localEntry = localCache.get(key);
        if (localEntry != null && !localEntry.isExpired()) {
            localEntry.recordAccess();
            hitCount.incrementAndGet();
            return localEntry.getValue();
        }
        
        // 2. 尝试从Redis缓存获取
        String redisKey = REDIS_CACHE_KEY_PREFIX + key;
        List<Float> redisValue = (List<Float>) redisTemplate.opsForValue().get(redisKey);
        if (redisValue != null) {
            // 更新本地缓存
            updateLocalCache(key, redisValue);
            hitCount.incrementAndGet();
            return redisValue;
        }
        
        missCount.incrementAndGet();
        return null;
    }
    
    public void put(String key, List<Float> value) {
        // 1. 更新本地缓存
        updateLocalCache(key, value);
        
        // 2. 异步更新Redis缓存
        CompletableFuture.runAsync(() -> {
            try {
                String redisKey = REDIS_CACHE_KEY_PREFIX + key;
                
                // 检查Redis缓存大小
                Long cacheSize = redisTemplate.opsForValue().size(REDIS_CACHE_KEY_PREFIX + "*");
                if (cacheSize != null && cacheSize >= MAX_REDIS_CACHE_SIZE) {
                    // 使用淘汰策略选择要删除的缓存
                    Set<String> keys = redisTemplate.keys(REDIS_CACHE_KEY_PREFIX + "*");
                    if (keys != null) {
                        List<String> evictionKeys = evictionStrategy.selectEvictionEntries(
                            keys.stream().collect(Collectors.toMap(
                                k -> k,
                                k -> new CacheEntry((List<Float>) redisTemplate.opsForValue().get(k))
                            )),
                            MAX_REDIS_CACHE_SIZE
                        );
                        
                        if (!evictionKeys.isEmpty()) {
                            redisTemplate.delete(evictionKeys);
                            evictionCount.addAndGet(evictionKeys.size());
                        }
                    }
                }
                
                // 存入新缓存
                redisTemplate.opsForValue().set(redisKey, value, REDIS_CACHE_EXPIRE, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("更新Redis缓存失败", e);
            }
        });
    }
    
    public void batchPut(Map<String, List<Float>> entries) {
        // 1. 更新本地缓存
        entries.forEach(this::updateLocalCache);
        
        // 2. 异步更新Redis缓存
        CompletableFuture.runAsync(() -> {
            try {
                // 检查Redis缓存大小
                Long cacheSize = redisTemplate.opsForValue().size(REDIS_CACHE_KEY_PREFIX + "*");
                if (cacheSize != null && cacheSize >= MAX_REDIS_CACHE_SIZE) {
                    // 使用淘汰策略选择要删除的缓存
                    Set<String> keys = redisTemplate.keys(REDIS_CACHE_KEY_PREFIX + "*");
                    if (keys != null) {
                        List<String> evictionKeys = evictionStrategy.selectEvictionEntries(
                            keys.stream().collect(Collectors.toMap(
                                k -> k,
                                k -> new CacheEntry((List<Float>) redisTemplate.opsForValue().get(k))
                            )),
                            MAX_REDIS_CACHE_SIZE - entries.size()
                        );
                        
                        if (!evictionKeys.isEmpty()) {
                            redisTemplate.delete(evictionKeys);
                            evictionCount.addAndGet(evictionKeys.size());
                        }
                    }
                }
                
                // 批量存入新缓存
                Map<String, List<Float>> redisEntries = entries.entrySet().stream()
                    .collect(Collectors.toMap(
                        e -> REDIS_CACHE_KEY_PREFIX + e.getKey(),
                        Map.Entry::getValue
                    ));
                
                redisTemplate.opsForValue().multiSet(redisEntries);
                redisEntries.keySet().forEach(key -> 
                    redisTemplate.expire(key, REDIS_CACHE_EXPIRE, TimeUnit.SECONDS)
                );
            } catch (Exception e) {
                log.error("批量更新Redis缓存失败", e);
            }
        });
    }
    
    public Map<String, List<Float>> batchGet(List<String> keys) {
        Map<String, List<Float>> result = new HashMap<>();
        List<String> missingKeys = new ArrayList<>();
        
        // 1. 尝试从本地缓存获取
        for (String key : keys) {
            CacheEntry localEntry = localCache.get(key);
            if (localEntry != null && !localEntry.isExpired()) {
                localEntry.recordAccess();
                result.put(key, localEntry.getValue());
                hitCount.incrementAndGet();
            } else {
                missingKeys.add(key);
                missCount.incrementAndGet();
            }
        }
        
        // 2. 尝试从Redis缓存获取缺失的键
        if (!missingKeys.isEmpty()) {
            List<String> redisKeys = missingKeys.stream()
                .map(key -> REDIS_CACHE_KEY_PREFIX + key)
                .collect(Collectors.toList());
            
            List<Object> redisValues = redisTemplate.opsForValue().multiGet(redisKeys);
            
            for (int i = 0; i < missingKeys.size(); i++) {
                String key = missingKeys.get(i);
                List<Float> value = (List<Float>) redisValues.get(i);
                if (value != null) {
                    result.put(key, value);
                    updateLocalCache(key, value);
                    hitCount.incrementAndGet();
                }
            }
        }
        
        return result;
    }
    
    private void updateLocalCache(String key, List<Float> value) {
        // 检查本地缓存大小
        if (localCache.size() >= LOCAL_CACHE_SIZE) {
            // 使用淘汰策略选择要删除的缓存
            List<String> evictionKeys = evictionStrategy.selectEvictionEntries(localCache, LOCAL_CACHE_SIZE);
            evictionKeys.forEach(localCache::remove);
            evictionCount.addAndGet(evictionKeys.size());
        }
        
        // 更新缓存
        localCache.put(key, new CacheEntry(value));
    }
    
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("localCacheSize", localCache.size());
        stats.put("hitCount", hitCount.get());
        stats.put("missCount", missCount.get());
        stats.put("evictionCount", evictionCount.get());
        stats.put("hitRate", calculateHitRate());
        stats.put("evictionStrategy", evictionStrategy.getStrategyName());
        return stats;
    }
    
    private double calculateHitRate() {
        long total = hitCount.get() + missCount.get();
        return total > 0 ? (double) hitCount.get() / total : 0.0;
    }
    
    @RequiredArgsConstructor
    private static class CacheEntry {
        private final List<Float> value;
        private final long timestamp = System.currentTimeMillis();
        private long lastAccessTime = System.currentTimeMillis();
        private int accessCount = 0;
        
        public List<Float> getValue() {
            return value;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public int getAccessCount() {
            return accessCount;
        }
        
        public void recordAccess() {
            lastAccessTime = System.currentTimeMillis();
            accessCount++;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > LOCAL_CACHE_EXPIRE * 1000;
        }
    }
} 