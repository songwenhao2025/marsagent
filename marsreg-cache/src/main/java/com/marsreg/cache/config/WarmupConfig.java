package com.marsreg.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "cache.warmup")
public class WarmupConfig {
    
    private boolean enabled = true;
    private int batchSize = 100;
    private int maxRetries = 3;
    private long retryDelay = 1000;
    
    private Map<String, CacheTypeConfig> types;
    
    @Data
    public static class CacheTypeConfig {
        private boolean enabled = true;
        private List<String> keys;
        private String keyPattern;
        private int concurrency = 1;
        private long timeout = 30000;
    }
} 