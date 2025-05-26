package com.marsreg.document.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@EnableCaching
public class SearchCacheConfig {
    
    public static final String SEARCH_RESULT_CACHE = "searchResultCache";
    public static final String SEARCH_SUGGESTION_CACHE = "searchSuggestionCache";
    public static final String SEARCH_HIGHLIGHT_CACHE = "searchHighlightCache";
    
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(
            SEARCH_RESULT_CACHE,
            SEARCH_SUGGESTION_CACHE,
            SEARCH_HIGHLIGHT_CACHE
        ));
        return cacheManager;
    }
} 