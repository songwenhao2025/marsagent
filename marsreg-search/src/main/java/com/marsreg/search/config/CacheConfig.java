package com.marsreg.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${search.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${search.cache.expire-time:3600}")
    private int expireTime;

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "searchResults",      // 检索结果缓存
            "vectorResults",      // 向量检索结果缓存
            "keywordResults",     // 关键词检索结果缓存
            "hybridResults"       // 混合检索结果缓存
        ));
        return cacheManager;
    }
} 