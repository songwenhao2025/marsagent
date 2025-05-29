package com.marsreg.cache.service.impl;

import com.marsreg.cache.service.CacheCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存清理服务实现
 */
@Slf4j
@Service
public class CacheCleanupServiceImpl implements CacheCleanupService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final Map<String, CleanupStatus> cleanupStatuses = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public CleanupResult cleanupByType(String type) {
        CleanupResult result = new CleanupResult();
        result.setType(type);
        
        try {
            Set<String> keys = redisTemplate.keys(type + ":*");
            if (keys != null && !keys.isEmpty()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (String key : keys) {
                        try {
                            Boolean deleted = redisTemplate.delete(key);
                            if (Boolean.TRUE.equals(deleted)) {
                                result.setSuccessCount(result.getSuccessCount() + 1);
                            } else {
                                result.setFailureCount(result.getFailureCount() + 1);
                            }
                        } catch (Exception e) {
                            log.error("清理缓存失败 - 键: {}", key, e);
                            result.setFailureCount(result.getFailureCount() + 1);
                        }
                    }
                }, executorService);
                
                future.get(5, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("清理缓存失败 - 类型: {}", type, e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            updateCleanupStatus(type, result);
        }
        
        return result;
    }

    @Override
    public CleanupResult cleanupAll() {
        CleanupResult result = new CleanupResult();
        result.setType("all");
        
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (String key : keys) {
                        try {
                            Boolean deleted = redisTemplate.delete(key);
                            if (Boolean.TRUE.equals(deleted)) {
                                result.setSuccessCount(result.getSuccessCount() + 1);
                            } else {
                                result.setFailureCount(result.getFailureCount() + 1);
                            }
                        } catch (Exception e) {
                            log.error("清理缓存失败 - 键: {}", key, e);
                            result.setFailureCount(result.getFailureCount() + 1);
                        }
                    }
                }, executorService);
                
                future.get(5, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("清理所有缓存失败", e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            updateCleanupStatus("all", result);
        }
        
        return result;
    }

    @Override
    public CleanupResult cleanupByPattern(String pattern) {
        CleanupResult result = new CleanupResult();
        result.setPattern(pattern);
        
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (String key : keys) {
                        try {
                            Boolean deleted = redisTemplate.delete(key);
                            if (Boolean.TRUE.equals(deleted)) {
                                result.setSuccessCount(result.getSuccessCount() + 1);
                            } else {
                                result.setFailureCount(result.getFailureCount() + 1);
                            }
                        } catch (Exception e) {
                            log.error("清理缓存失败 - 键: {}", key, e);
                            result.setFailureCount(result.getFailureCount() + 1);
                        }
                    }
                }, executorService);
                
                future.get(5, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("清理缓存失败 - 模式: {}", pattern, e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            updateCleanupStatus(pattern, result);
        }
        
        return result;
    }

    @Override
    public CleanupResult cleanupByKeys(List<String> keys) {
        CleanupResult result = new CleanupResult();
        
        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (String key : keys) {
                    try {
                        Boolean deleted = redisTemplate.delete(key);
                        if (Boolean.TRUE.equals(deleted)) {
                            result.setSuccessCount(result.getSuccessCount() + 1);
                        } else {
                            result.setFailureCount(result.getFailureCount() + 1);
                        }
                    } catch (Exception e) {
                        log.error("清理缓存失败 - 键: {}", key, e);
                        result.setFailureCount(result.getFailureCount() + 1);
                    }
                }
            }, executorService);
            
            future.get(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("清理缓存失败 - 键列表", e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            updateCleanupStatus("keys", result);
        }
        
        return result;
    }

    @Override
    public CleanupResult cleanupExpired() {
        CleanupResult result = new CleanupResult();
        result.setType("expired");
        
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (String key : keys) {
                        try {
                            Long ttl = redisTemplate.getExpire(key);
                            if (ttl != null && ttl <= 0) {
                                Boolean deleted = redisTemplate.delete(key);
                                if (Boolean.TRUE.equals(deleted)) {
                                    result.setSuccessCount(result.getSuccessCount() + 1);
                                } else {
                                    result.setFailureCount(result.getFailureCount() + 1);
                                }
                            }
                        } catch (Exception e) {
                            log.error("清理过期缓存失败 - 键: {}", key, e);
                            result.setFailureCount(result.getFailureCount() + 1);
                        }
                    }
                }, executorService);
                
                future.get(5, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            updateCleanupStatus("expired", result);
        }
        
        return result;
    }

    @Override
    public CleanupStatus getCleanupStatus(String type) {
        return cleanupStatuses.get(type);
    }

    @Override
    public CleanupStatus getTotalCleanupStatus() {
        return new CleanupStatus() {
            @Override
            public int getSuccessCount() {
                return cleanupStatuses.values().stream()
                    .mapToInt(CleanupStatus::getSuccessCount)
                    .sum();
            }
            
            @Override
            public int getFailureCount() {
                return cleanupStatuses.values().stream()
                    .mapToInt(CleanupStatus::getFailureCount)
                    .sum();
            }
            
            @Override
            public long getStartTime() {
                return cleanupStatuses.values().stream()
                    .mapToLong(CleanupStatus::getStartTime)
                    .min()
                    .orElse(0);
            }
            
            @Override
            public long getEndTime() {
                return cleanupStatuses.values().stream()
                    .mapToLong(CleanupStatus::getEndTime)
                    .max()
                    .orElse(0);
            }
            
            @Override
            public long getDuration() {
                return getEndTime() - getStartTime();
            }
            
            @Override
            public double getSuccessRate() {
                int total = getSuccessCount() + getFailureCount();
                return total > 0 ? (double) getSuccessCount() / total * 100 : 0;
            }
        };
    }

    private void updateCleanupStatus(String type, CleanupResult result) {
        cleanupStatuses.put(type, new CleanupStatus() {
            @Override
            public int getSuccessCount() {
                return result.getSuccessCount();
            }
            
            @Override
            public int getFailureCount() {
                return result.getFailureCount();
            }
            
            @Override
            public long getStartTime() {
                return result.getStartTime();
            }
            
            @Override
            public long getEndTime() {
                return result.getEndTime();
            }
            
            @Override
            public long getDuration() {
                return result.getDuration();
            }
            
            @Override
            public double getSuccessRate() {
                return result.getSuccessRate();
            }
        });
    }
} 