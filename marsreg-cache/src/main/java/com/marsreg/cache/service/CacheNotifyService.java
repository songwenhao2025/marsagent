package com.marsreg.cache.service;

public interface CacheNotifyService {
    
    /**
     * 发送通知
     *
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param level 通知级别
     */
    void notify(String type, String title, String content, NotifyLevel level);
    
    /**
     * 通知级别
     */
    enum NotifyLevel {
        INFO,
        WARN,
        ERROR
    }
} 