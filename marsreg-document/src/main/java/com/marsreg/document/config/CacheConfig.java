package com.marsreg.document.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // 缓存名称常量
    public static final String DOCUMENT_CACHE = "document_cache";
    public static final String DOCUMENT_PROCESS_CACHE = "document_process_cache";
    public static final String DOCUMENT_VERSION_CACHE = "document_version_cache";
    public static final String DOCUMENT_CHUNKS_CACHE = "document_chunks_cache";
    public static final String DOCUMENT_CONTENT_CACHE = "document_content_cache";

    // 缓存过期时间（秒）
    public static final long DOCUMENT_CACHE_TTL = 3600;
    public static final long DOCUMENT_PROCESS_CACHE_TTL = 1800;
    public static final long DOCUMENT_VERSION_CACHE_TTL = 7200;
    public static final long DOCUMENT_CHUNKS_CACHE_TTL = 3600;
    public static final long DOCUMENT_CONTENT_CACHE_TTL = 3600;
} 