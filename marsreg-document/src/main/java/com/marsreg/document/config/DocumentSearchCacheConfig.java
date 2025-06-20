package com.marsreg.document.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class DocumentSearchCacheConfig {
    // 缓存名称常量
    public static final String SEARCH_RESULT_CACHE = "search_result_cache";
    public static final String SEARCH_SUGGESTION_CACHE = "search_suggestion_cache";
    public static final String SEARCH_HISTORY_CACHE = "search_history_cache";
    public static final String SEARCH_HIGHLIGHT_CACHE = "search_highlight_cache";

    // 缓存过期时间（秒）
    public static final long SEARCH_RESULT_CACHE_TTL = 1800;
    public static final long SEARCH_SUGGESTION_CACHE_TTL = 3600;
    public static final long SEARCH_HISTORY_CACHE_TTL = 86400;
    public static final long SEARCH_HIGHLIGHT_CACHE_TTL = 1800;
} 