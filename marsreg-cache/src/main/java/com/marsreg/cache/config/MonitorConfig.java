package com.marsreg.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cache.monitor")
public class MonitorConfig {
    
    private boolean enabled = true;
    private long interval = 60000; // 监控间隔，默认1分钟
    private boolean alertEnabled = true;
    private AlertConfig alert = new AlertConfig();
    
    @Data
    public static class AlertConfig {
        private double hitRateThreshold = 80.0; // 命中率阈值
        private long maxLoadTime = 1000; // 最大加载时间（毫秒）
        private long maxEvictionCount = 1000; // 最大驱逐次数
        private boolean notifyEnabled = true;
        private String notifyType = "log"; // log, email, webhook
        private String notifyTarget; // 通知目标（邮箱或webhook地址）
    }
} 