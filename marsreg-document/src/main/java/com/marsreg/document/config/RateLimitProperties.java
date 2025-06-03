package com.marsreg.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "marsreg.document.rate-limit")
public class RateLimitProperties {
    
    private RateLimitConfig defaultConfig = new RateLimitConfig();
    private RateLimitConfig upload = new RateLimitConfig();
    private RateLimitConfig download = new RateLimitConfig();
    
    @Data
    public static class RateLimitConfig {
        private int limit = 10;
        private int timeWindow = 60;
    }
} 