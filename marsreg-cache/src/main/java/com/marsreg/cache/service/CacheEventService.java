package com.marsreg.cache.service;

import java.util.List;
import java.util.Map;

/**
 * 缓存事件服务接口
 */
public interface CacheEventService {
    
    /**
     * 发布缓存事件
     * @param type 事件类型
     * @param data 事件数据
     */
    void publishEvent(String type, Map<String, Object> data);
    
    /**
     * 订阅缓存事件
     * @param type 事件类型
     * @param listener 事件监听器
     */
    void subscribe(String type, CacheEventListener listener);
    
    /**
     * 取消订阅缓存事件
     * @param type 事件类型
     * @param listener 事件监听器
     */
    void unsubscribe(String type, CacheEventListener listener);
    
    /**
     * 获取事件历史记录
     * @param type 事件类型
     * @param limit 限制数量
     * @return 事件历史记录
     */
    List<Map<String, Object>> getEventHistory(String type, int limit);
    
    /**
     * 清除事件历史记录
     * @param type 事件类型
     */
    void clearEventHistory(String type);
    
    /**
     * 获取所有事件类型
     * @return 事件类型列表
     */
    List<String> getEventTypes();
    
    /**
     * 获取事件统计信息
     * @return 事件统计信息
     */
    Map<String, Long> getEventStats();
    
    /**
     * 缓存事件监听器接口
     */
    interface CacheEventListener {
        /**
         * 处理缓存事件
         * @param type 事件类型
         * @param data 事件数据
         */
        void onEvent(String type, Map<String, Object> data);
    }
} 