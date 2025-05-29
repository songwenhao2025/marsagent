package com.marsreg.cache.service.impl;

import com.marsreg.cache.service.CacheSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheSyncServiceImpl implements CacheSyncService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentHashMap<String, SyncStatus> syncStatuses = new ConcurrentHashMap<>();

    @Override
    public long sync(String type) {
        try {
            String pattern = type + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                SyncStatus status = new SyncStatusImpl();
                syncStatuses.put(type, status);
                status.start();
                
                long syncedCount = 0;
                long failedCount = 0;
                
                for (String key : keys) {
                    try {
                        Object value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            redisTemplate.opsForValue().set(key, value);
                            syncedCount++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to sync cache for key: " + key, e);
                        failedCount++;
                    }
                }
                
                status.complete(syncedCount, failedCount);
                log.info("Synced {} cache entries for type: {}", syncedCount, type);
                return syncedCount;
            }
        } catch (Exception e) {
            log.error("Failed to sync cache for type: " + type, e);
        }
        return 0;
    }

    @Override
    public long syncAll() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                SyncStatus status = new SyncStatusImpl();
                syncStatuses.put("all", status);
                status.start();
                
                long syncedCount = 0;
                long failedCount = 0;
                
                for (String key : keys) {
                    try {
                        Object value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            redisTemplate.opsForValue().set(key, value);
                            syncedCount++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to sync cache for key: " + key, e);
                        failedCount++;
                    }
                }
                
                status.complete(syncedCount, failedCount);
                log.info("Synced all cache entries: {}", syncedCount);
                return syncedCount;
            }
        } catch (Exception e) {
            log.error("Failed to sync all cache", e);
        }
        return 0;
    }

    @Override
    public long syncByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                SyncStatus status = new SyncStatusImpl();
                syncStatuses.put(pattern, status);
                status.start();
                
                long syncedCount = 0;
                long failedCount = 0;
                
                for (String key : keys) {
                    try {
                        Object value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            redisTemplate.opsForValue().set(key, value);
                            syncedCount++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to sync cache for key: " + key, e);
                        failedCount++;
                    }
                }
                
                status.complete(syncedCount, failedCount);
                log.info("Synced {} cache entries for pattern: {}", syncedCount, pattern);
                return syncedCount;
            }
        } catch (Exception e) {
            log.error("Failed to sync cache for pattern: " + pattern, e);
        }
        return 0;
    }

    @Override
    public SyncStatus getSyncStatus(String type) {
        return syncStatuses.getOrDefault(type, new SyncStatusImpl());
    }

    @Override
    public SyncStatus getTotalSyncStatus() {
        return syncStatuses.getOrDefault("all", new SyncStatusImpl());
    }

    private static class SyncStatusImpl implements SyncStatus {
        private final AtomicLong syncedCount = new AtomicLong(0);
        private final AtomicLong failedCount = new AtomicLong(0);
        private final AtomicLong startTime = new AtomicLong(0);
        private final AtomicLong endTime = new AtomicLong(0);

        public void start() {
            startTime.set(System.currentTimeMillis());
        }

        public void complete(long synced, long failed) {
            syncedCount.set(synced);
            failedCount.set(failed);
            endTime.set(System.currentTimeMillis());
        }

        @Override
        public long getSyncedCount() {
            return syncedCount.get();
        }

        @Override
        public long getFailedCount() {
            return failedCount.get();
        }

        @Override
        public long getStartTime() {
            return startTime.get();
        }

        @Override
        public long getEndTime() {
            return endTime.get();
        }

        @Override
        public long getDuration() {
            return endTime.get() - startTime.get();
        }

        @Override
        public double getSuccessRate() {
            long total = syncedCount.get() + failedCount.get();
            return total == 0 ? 0.0 : (double) syncedCount.get() / total * 100;
        }
    }
} 