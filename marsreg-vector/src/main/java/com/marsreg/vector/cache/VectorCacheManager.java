package com.marsreg.vector.cache;

import com.marsreg.vector.cache.eviction.CacheEvictionStrategy;
import com.marsreg.vector.cache.eviction.LRUEvictionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

@Slf4j
@Component
public class VectorCacheManager {
    private final Map<String, CacheEntry> cache;
    private final CacheEvictionStrategy evictionStrategy;
    
    @Value("${vector.cache.max-size:1000}")
    private int maxSize;
    
    @Value("${vector.cache.max-memory:1000000}")
    private long maxMemory;
    
    private long currentMemory;

    public VectorCacheManager() {
        this.cache = new ConcurrentHashMap<>();
        this.evictionStrategy = new LRUEvictionStrategy();
        this.currentMemory = 0;
    }

    @PostConstruct
    public void init() {
        log.info("初始化向量缓存管理器: maxSize={}, maxMemory={}", maxSize, maxMemory);
    }

    public float[] getVector(String text) {
        CacheEntry entry = cache.get(text);
        if (entry != null && !entry.isExpired()) {
            return entry.getVector();
        }
        return null;
    }

    public void put(String key, float[] value) {
        CacheEntry entry = new CacheEntry(key, value);
        
        // 如果缓存已满，执行淘汰策略
        while (cache.size() >= maxSize || currentMemory + entry.getSize() > maxMemory) {
            String evictedKey = evictionStrategy.evict(cache);
            if (evictedKey != null) {
                CacheEntry evictedEntry = cache.remove(evictedKey);
                currentMemory -= evictedEntry.getSize();
            } else {
                break;
            }
        }
        
        cache.put(key, entry);
        currentMemory += entry.getSize();
    }

    public void remove(String key) {
        CacheEntry entry = cache.remove(key);
        if (entry != null) {
            currentMemory -= entry.getSize();
        }
    }

    public void clear() {
        cache.clear();
        currentMemory = 0;
    }

    public int size() {
        return cache.size();
    }

    public long getCurrentMemory() {
        return currentMemory;
    }

    public Map<String, float[]> batchGet(List<String> keys) {
        Map<String, float[]> result = new HashMap<>();
        for (String key : keys) {
            float[] value = getVector(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    public void batchPut(Map<String, float[]> map) {
        for (Map.Entry<String, float[]> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", size());
        stats.put("currentMemory", getCurrentMemory());
        stats.put("maxSize", maxSize);
        stats.put("maxMemory", maxMemory);
        return stats;
    }
} 