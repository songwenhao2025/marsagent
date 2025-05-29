package com.marsreg.cache.service.impl;

import com.marsreg.cache.service.CacheEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 缓存事件服务实现
 */
@Slf4j
@Service
public class CacheEventServiceImpl implements CacheEventService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final Map<String, List<CacheEventListener>> listeners = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> eventHistory = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventCounts = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 1000;

    @Override
    public void publishEvent(String type, Map<String, Object> data) {
        // 添加时间戳
        data.put("timestamp", new Date());
        
        // 记录事件历史
        eventHistory.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                   .add(new HashMap<>(data));
        
        // 限制历史记录大小
        List<Map<String, Object>> history = eventHistory.get(type);
        if (history.size() > MAX_HISTORY_SIZE) {
            history.subList(0, history.size() - MAX_HISTORY_SIZE).clear();
        }
        
        // 更新事件计数
        eventCounts.computeIfAbsent(type, k -> new AtomicLong(0)).incrementAndGet();
        
        // 通知监听器
        List<CacheEventListener> typeListeners = listeners.get(type);
        if (typeListeners != null) {
            typeListeners.forEach(listener -> {
                try {
                    listener.onEvent(type, data);
                } catch (Exception e) {
                    log.error("处理缓存事件失败 - 类型: {}, 监听器: {}", type, listener, e);
                }
            });
        }
        
        log.debug("发布缓存事件 - 类型: {}, 数据: {}", type, data);
    }

    @Override
    public void subscribe(String type, CacheEventListener listener) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.info("订阅缓存事件 - 类型: {}, 监听器: {}", type, listener);
    }

    @Override
    public void unsubscribe(String type, CacheEventListener listener) {
        List<CacheEventListener> typeListeners = listeners.get(type);
        if (typeListeners != null) {
            typeListeners.remove(listener);
            log.info("取消订阅缓存事件 - 类型: {}, 监听器: {}", type, listener);
        }
    }

    @Override
    public List<Map<String, Object>> getEventHistory(String type, int limit) {
        List<Map<String, Object>> history = eventHistory.getOrDefault(type, Collections.emptyList());
        return history.stream()
                     .skip(Math.max(0, history.size() - limit))
                     .collect(Collectors.toList());
    }

    @Override
    public void clearEventHistory(String type) {
        eventHistory.remove(type);
        log.info("清除缓存事件历史记录 - 类型: {}", type);
    }

    @Override
    public List<String> getEventTypes() {
        return new ArrayList<>(listeners.keySet());
    }

    @Override
    public Map<String, Long> getEventStats() {
        Map<String, Long> stats = new HashMap<>();
        eventCounts.forEach((type, count) -> stats.put(type, count.get()));
        return stats;
    }
} 