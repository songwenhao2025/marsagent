package com.marsreg.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "marsreg.document.chunking")
public class ChunkingConfig {
    /**
     * 默认最大块大小（字符数）
     */
    private int defaultMaxChunkSize = 1000;

    /**
     * 默认最小块大小（字符数）
     */
    private int defaultMinChunkSize = 200;

    /**
     * 默认重叠大小（字符数）
     */
    private int defaultOverlapSize = 100;

    /**
     * 是否启用智能分块
     */
    private boolean enableSmartChunking = true;

    /**
     * 是否在句子边界分割
     */
    private boolean splitOnSentenceBoundary = true;

    /**
     * 是否在段落边界分割
     */
    private boolean splitOnParagraphBoundary = true;

    /**
     * 是否保持段落完整性
     */
    private boolean preserveParagraphIntegrity = true;
} 