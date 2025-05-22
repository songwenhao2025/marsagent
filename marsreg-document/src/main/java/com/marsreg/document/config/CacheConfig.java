package com.marsreg.document.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {
    
    public static final String DOCUMENT_CHUNKS_CACHE = "documentChunks";
    public static final String DOCUMENT_CONTENT_CACHE = "documentContent";
    
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(DOCUMENT_CHUNKS_CACHE, DOCUMENT_CONTENT_CACHE));
        return cacheManager;
    }
} 