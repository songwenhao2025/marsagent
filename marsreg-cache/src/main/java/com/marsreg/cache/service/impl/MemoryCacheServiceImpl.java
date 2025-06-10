package com.marsreg.cache.service.impl;

import com.marsreg.cache.config.CacheProperties;
import com.marsreg.cache.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class MemoryCacheServiceImpl implements CacheService {
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final CacheProperties cacheProperties;
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    public MemoryCacheServiceImpl(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            hitCount.incrementAndGet();
            return (T) entry.getValue();
        }
        missCount.incrementAndGet();
        return null;
    }

    @Override
    public <T> void set(String key, T value) {
        set(key, value, cacheProperties.getMemory().getDefaultExpire(), TimeUnit.SECONDS);
    }

    @Override
    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        if (cache.size() >= cacheProperties.getMemory().getMaxSize()) {
            evictExpiredEntries();
        }
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + unit.toMillis(timeout)));
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public void delete(Collection<String> keys) {
        keys.forEach(cache::remove);
    }

    @Override
    public boolean exists(String key) {
        CacheEntry entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.setExpireTime(System.currentTimeMillis() + unit.toMillis(timeout));
        }
    }

    @Override
    public long getExpire(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return TimeUnit.MILLISECONDS.toSeconds(entry.getExpireTime() - System.currentTimeMillis());
        }
        return -1;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("size", size());
        stats.put("hitCount", hitCount.get());
        stats.put("missCount", missCount.get());
        stats.put("hitRate", calculateHitRate());
        return stats;
    }

    @Override
    public void clear() {
        cache.clear();
        hitCount.set(0);
        missCount.set(0);
    }

    @Override
    public long size() {
        evictExpiredEntries();
        return cache.size();
    }

    private void evictExpiredEntries() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private double calculateHitRate() {
        long total = hitCount.get() + missCount.get();
        return total > 0 ? (double) hitCount.get() / total : 0;
    }

    private static class CacheEntry {
        private final Object value;
        private long expireTime;

        public CacheEntry(Object value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public Object getValue() {
            return value;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(long expireTime) {
            this.expireTime = expireTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
} 