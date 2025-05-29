package com.marsreg.cache.service.impl;

import com.marsreg.cache.config.CacheConfig;
import com.marsreg.cache.service.CacheStatsService;
import com.marsreg.cache.service.MultiLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存统计服务实现
 */
@Slf4j
@Service
public class CacheStatsServiceImpl implements CacheStatsService {

    @Autowired
    private MultiLevelCacheService cacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheConfig cacheConfig;

    private final ConcurrentHashMap<String, CacheStatsImpl> statsMap = new ConcurrentHashMap<>();

    @Override
    public CacheStats getStats(String type) {
        return statsMap.computeIfAbsent(type, k -> new CacheStatsImpl());
    }

    @Override
    public CacheStats getTotalStats() {
        CacheStatsImpl totalStats = new CacheStatsImpl();
        statsMap.values().forEach(stats -> {
            totalStats.addHitCount(stats.getHitCount());
            totalStats.addMissCount(stats.getMissCount());
            totalStats.addLoadSuccessCount(stats.getLoadSuccessCount());
            totalStats.addLoadFailureCount(stats.getLoadFailureCount());
            totalStats.addTotalLoadTime(stats.getTotalLoadTime());
            totalStats.addEvictionCount(stats.getEvictionCount());
        });
        return totalStats;
    }

    @Override
    public void resetStats(String type) {
        statsMap.put(type, new CacheStatsImpl());
        log.info("重置缓存统计信息: {}", type);
    }

    @Override
    public void resetAllStats() {
        statsMap.clear();
        log.info("重置所有缓存统计信息");
    }

    @Override
    public Map<String, Long> getSizeStats() {
        Map<String, Long> sizeStats = new ConcurrentHashMap<>();
        cacheConfig.getWarmup().getTypes().forEach((type, config) -> {
            String pattern = config.getKeyPattern();
            Set<String> keys = redisTemplate.keys(pattern);
            sizeStats.put(type, keys != null ? (long) keys.size() : 0L);
        });
        return sizeStats;
    }

    @Override
    public Map<String, Double> getHitRateStats() {
        Map<String, Double> hitRateStats = new ConcurrentHashMap<>();
        statsMap.forEach((type, stats) -> hitRateStats.put(type, stats.getHitRate()));
        return hitRateStats;
    }

    @Override
    public Map<String, Long> getLoadTimeStats() {
        Map<String, Long> loadTimeStats = new ConcurrentHashMap<>();
        statsMap.forEach((type, stats) -> loadTimeStats.put(type, stats.getTotalLoadTime()));
        return loadTimeStats;
    }

    private static class CacheStatsImpl implements CacheStats {
        private final AtomicLong hitCount = new AtomicLong(0);
        private final AtomicLong missCount = new AtomicLong(0);
        private final AtomicLong loadSuccessCount = new AtomicLong(0);
        private final AtomicLong loadFailureCount = new AtomicLong(0);
        private final AtomicLong totalLoadTime = new AtomicLong(0);
        private final AtomicLong evictionCount = new AtomicLong(0);

        public void incrementHitCount() {
            hitCount.incrementAndGet();
        }

        public void incrementMissCount() {
            missCount.incrementAndGet();
        }

        public void incrementLoadSuccessCount() {
            loadSuccessCount.incrementAndGet();
        }

        public void incrementLoadFailureCount() {
            loadFailureCount.incrementAndGet();
        }

        public void addLoadTime(long time) {
            totalLoadTime.addAndGet(time);
        }

        public void incrementEvictionCount() {
            evictionCount.incrementAndGet();
        }

        public void addHitCount(long count) {
            hitCount.addAndGet(count);
        }

        public void addMissCount(long count) {
            missCount.addAndGet(count);
        }

        public void addLoadSuccessCount(long count) {
            loadSuccessCount.addAndGet(count);
        }

        public void addLoadFailureCount(long count) {
            loadFailureCount.addAndGet(count);
        }

        public void addTotalLoadTime(long time) {
            totalLoadTime.addAndGet(time);
        }

        public void addEvictionCount(long count) {
            evictionCount.addAndGet(count);
        }

        @Override
        public long getHitCount() {
            return hitCount.get();
        }

        @Override
        public long getMissCount() {
            return missCount.get();
        }

        @Override
        public long getLoadSuccessCount() {
            return loadSuccessCount.get();
        }

        @Override
        public long getLoadFailureCount() {
            return loadFailureCount.get();
        }

        @Override
        public long getTotalLoadTime() {
            return totalLoadTime.get();
        }

        @Override
        public long getEvictionCount() {
            return evictionCount.get();
        }

        @Override
        public double getHitRate() {
            long total = hitCount.get() + missCount.get();
            return total > 0 ? (double) hitCount.get() / total * 100 : 0;
        }

        @Override
        public double getAverageLoadTime() {
            long total = loadSuccessCount.get();
            return total > 0 ? (double) totalLoadTime.get() / total : 0;
        }

        @Override
        public double getLoadSuccessRate() {
            long total = loadSuccessCount.get() + loadFailureCount.get();
            return total > 0 ? (double) loadSuccessCount.get() / total * 100 : 0;
        }
    }
} 