package com.marsreg.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "cache.degrade")
public class DegradeConfig {
    
    /**
     * 是否启用降级
     */
    private boolean enabled = true;
    
    /**
     * 时间窗口（秒）
     */
    @Min(value = 1, message = "时间窗口必须大于0")
    private long timeWindow = 60;
    
    /**
     * 最小请求数
     */
    @Min(value = 1, message = "最小请求数必须大于0")
    private int minRequestCount = 10;
    
    /**
     * 错误率阈值（百分比）
     */
    @Min(value = 0, message = "错误率阈值不能小于0")
    private double errorRateThreshold = 50.0;
    
    /**
     * 降级持续时间（秒）
     */
    @Min(value = 1, message = "降级持续时间必须大于0")
    private long degradeDuration = 300;
    
    /**
     * 是否自动恢复
     */
    private boolean autoRecover = true;
    
    /**
     * 恢复检查间隔（秒）
     */
    @Min(value = 1, message = "恢复检查间隔必须大于0")
    private long recoverCheckInterval = 60;
} 