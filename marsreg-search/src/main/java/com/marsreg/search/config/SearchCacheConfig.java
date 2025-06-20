package com.marsreg.search.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableCaching
public class SearchCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "document_search",
            "document_advanced_search",
            "document_highlight_search",
            "document_similar_search",
            "document_suggestions",
            "document_aggregations",
            "document_field_search",
            "document_category_search",
            "document_tags_search",
            "document_time_search",
            "document_author_search",
            "document_status_search",
            "document_content_type_search",
            "document_metadata_search",
            "document_vector_search",
            "document_combination_search"
        ));
        return cacheManager;
    }
} 