package com.marsreg.cache.service.impl;

import com.marsreg.cache.service.CacheWarmupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存预热服务实现
 */
@Slf4j
@Service
public class CacheWarmupServiceImpl implements CacheWarmupService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final Map<String, WarmupStatus> warmupStatuses = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public WarmupResult warmupByType(String type) {
        WarmupResult result = new WarmupResult();
        result.setType(type);
        
        try {
            Set<String> keys = redisTemplate.keys(type + ":*");
            if (keys != null && !keys.isEmpty()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (String key : keys) {
                        try {
                            Object value = redisTemplate.opsForValue().get(key);
                            if (value != null) {
                                result.setSuccessCount(result.getSuccessCount() + 1);
                            } else {
                                result.setFailureCount(result.getFailureCount() + 1);
                            }
                        } catch (Exception e) {
                            log.error("预热缓存失败 - 键: {}", key, e);
                            result.setFailureCount(result.getFailureCount() + 1);
                        }
                    }
                }, executorService);
                
                future.get(5, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("预热缓存失败 - 类型: {}", type, e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            updateWarmupStatus(type, result);
        }
        
        return result;
    }

    @Override
    public WarmupResult warmupAll() {
        WarmupResult result = new WarmupResult();
        result.setType("all");
        
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (String key : keys) {
                        try {
                            Object value = redisTemplate.opsForValue().get(key);
                            if (value != null) {
                                result.setSuccessCount(result.getSuccessCount() + 1);
                            } else {
                                result.setFailureCount(result.getFailureCount() + 1);
                            }
                        } catch (Exception e) {
                            log.error("预热缓存失败 - 键: {}", key, e);
                            result.setFailureCount(result.getFailureCount() + 1);
                        }
                    }
                }, executorService);
                
                future.get(5, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("预热所有缓存失败", e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            updateWarmupStatus("all", result);
        }
        
        return result;
    }

    @Override
    public WarmupResult warmupByPattern(String pattern) {
        WarmupResult result = new WarmupResult();
        result.setPattern(pattern);
        
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (String key : keys) {
                        try {
                            Object value = redisTemplate.opsForValue().get(key);
                            if (value != null) {
                                result.setSuccessCount(result.getSuccessCount() + 1);
                            } else {
                                result.setFailureCount(result.getFailureCount() + 1);
                            }
                        } catch (Exception e) {
                            log.error("预热缓存失败 - 键: {}", key, e);
                            result.setFailureCount(result.getFailureCount() + 1);
                        }
                    }
                }, executorService);
                
                future.get(5, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("预热缓存失败 - 模式: {}", pattern, e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            updateWarmupStatus(pattern, result);
        }
        
        return result;
    }

    @Override
    public WarmupResult warmupFromData(String type, Map<String, Object> data) {
        WarmupResult result = new WarmupResult();
        result.setType(type);
        
        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    try {
                        String key = type + ":" + entry.getKey();
                        redisTemplate.opsForValue().set(key, entry.getValue());
                        result.setSuccessCount(result.getSuccessCount() + 1);
                    } catch (Exception e) {
                        log.error("预热缓存失败 - 键: {}", entry.getKey(), e);
                        result.setFailureCount(result.getFailureCount() + 1);
                    }
                }
            }, executorService);
            
            future.get(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("从数据源预热缓存失败 - 类型: {}", type, e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            updateWarmupStatus(type, result);
        }
        
        return result;
    }

    @Override
    public WarmupResult warmupFromKeys(String type, List<String> keys) {
        WarmupResult result = new WarmupResult();
        result.setType(type);
        
        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (String key : keys) {
                    try {
                        String fullKey = type + ":" + key;
                        Object value = redisTemplate.opsForValue().get(fullKey);
                        if (value != null) {
                            result.setSuccessCount(result.getSuccessCount() + 1);
                        } else {
                            result.setFailureCount(result.getFailureCount() + 1);
                        }
                    } catch (Exception e) {
                        log.error("预热缓存失败 - 键: {}", key, e);
                        result.setFailureCount(result.getFailureCount() + 1);
                    }
                }
            }, executorService);
            
            future.get(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("从键列表预热缓存失败 - 类型: {}", type, e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            updateWarmupStatus(type, result);
        }
        
        return result;
    }

    @Override
    public WarmupStatus getWarmupStatus(String type) {
        return warmupStatuses.get(type);
    }

    @Override
    public WarmupStatus getTotalWarmupStatus() {
        return new WarmupStatus() {
            @Override
            public int getSuccessCount() {
                return warmupStatuses.values().stream()
                    .mapToInt(WarmupStatus::getSuccessCount)
                    .sum();
            }
            
            @Override
            public int getFailureCount() {
                return warmupStatuses.values().stream()
                    .mapToInt(WarmupStatus::getFailureCount)
                    .sum();
            }
            
            @Override
            public long getStartTime() {
                return warmupStatuses.values().stream()
                    .mapToLong(WarmupStatus::getStartTime)
                    .min()
                    .orElse(0);
            }
            
            @Override
            public long getEndTime() {
                return warmupStatuses.values().stream()
                    .mapToLong(WarmupStatus::getEndTime)
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

    private void updateWarmupStatus(String type, WarmupResult result) {
        warmupStatuses.put(type, new WarmupStatus() {
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