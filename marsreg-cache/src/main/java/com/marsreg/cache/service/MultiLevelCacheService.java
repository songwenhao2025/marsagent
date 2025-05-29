package com.marsreg.cache.service;

import java.util.concurrent.Callable;

public interface MultiLevelCacheService {
    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @param type 值类型
     * @return 缓存值
     */
    <T> T get(String key, Class<T> type);
    
    /**
     * 获取缓存值，如果不存在则加载
     *
     * @param key 缓存键
     * @param type 值类型
     * @param valueLoader 值加载器
     * @return 缓存值
     */
    <T> T get(String key, Class<T> type, Callable<T> valueLoader);
    
    /**
     * 设置缓存值
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);
    
    /**
     * 删除缓存值
     *
     * @param key 缓存键
     */
    void evict(String key);
    
    /**
     * 清空所有缓存
     */
    void clear();
    
    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    CacheStats getStats();
    
    /**
     * 缓存统计信息
     */
    interface CacheStats {
        long getHitCount();
        long getMissCount();
        long getLoadSuccessCount();
        long getLoadFailureCount();
        long getTotalLoadTime();
        long getEvictionCount();
    }
} 