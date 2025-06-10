package com.marsreg.cache.service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface CacheService {
    /**
     * 获取缓存值
     * @param key 缓存键
     * @return 缓存值
     */
    <T> T get(String key);

    /**
     * 设置缓存值
     * @param key 缓存键
     * @param value 缓存值
     */
    <T> void set(String key, T value);

    /**
     * 设置缓存值，带过期时间
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    <T> void set(String key, T value, long timeout, TimeUnit unit);

    /**
     * 删除缓存
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 批量删除缓存
     * @param keys 缓存键集合
     */
    void delete(Collection<String> keys);

    /**
     * 判断缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置过期时间
     * @param key 缓存键
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    void expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取过期时间
     * @param key 缓存键
     * @return 过期时间（秒）
     */
    long getExpire(String key);

    /**
     * 获取缓存统计信息
     * @return 统计信息
     */
    Map<String, Object> getStats();

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 获取缓存大小
     * @return 缓存大小
     */
    long size();
} 