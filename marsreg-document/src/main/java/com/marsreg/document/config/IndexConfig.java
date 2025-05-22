package com.marsreg.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "marsreg.document.index")
public class IndexConfig {
    /**
     * 是否启用索引
     */
    private boolean enabled = true;

    /**
     * 索引刷新间隔（毫秒）
     */
    private long refreshInterval = 300000; // 5分钟

    /**
     * 索引批处理大小
     */
    private int batchSize = 100;

    /**
     * 是否异步索引
     */
    private boolean async = true;

    /**
     * 索引线程池大小
     */
    private int threadPoolSize = 4;
} 