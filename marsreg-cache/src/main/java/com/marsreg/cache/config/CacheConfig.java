package com.marsreg.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheConfig {
    
    /**
     * 本地缓存配置
     */
    private LocalConfig local = new LocalConfig();
    
    /**
     * 分布式缓存配置
     */
    private DistributedConfig distributed = new DistributedConfig();
    
    /**
     * 多级缓存配置
     */
    private MultiLevelConfig multiLevel = new MultiLevelConfig();

    /**
     * 缓存预热配置
     */
    private WarmupConfig warmup = new WarmupConfig();

    @Data
    public static class LocalConfig {
        /**
         * 是否启用本地缓存
         */
        private boolean enabled = true;
        
        /**
         * 本地缓存类型
         */
        private String type = "caffeine";
        
        /**
         * 本地缓存最大容量
         */
        private int maximumSize = 10000;
        
        /**
         * 本地缓存过期时间（秒）
         */
        private long expireAfterWrite = 3600;
        
        /**
         * 本地缓存刷新时间（秒）
         */
        private long refreshAfterWrite = 1800;
    }
    
    @Data
    public static class DistributedConfig {
        /**
         * 是否启用分布式缓存
         */
        private boolean enabled = true;
        
        /**
         * 分布式缓存类型
         */
        private String type = "redis";
        
        /**
         * 分布式缓存最大容量
         */
        private int maximumSize = 10000;
        
        /**
         * Redis配置
         */
        private RedisConfig redis = new RedisConfig();
        
        @Data
        public static class RedisConfig {
            /**
             * Redis主机
             */
            private String host = "localhost";
            
            /**
             * Redis端口
             */
            private int port = 6379;
            
            /**
             * Redis密码
             */
            private String password;
            
            /**
             * Redis数据库
             */
            private int database = 0;
            
            /**
             * Redis连接超时时间（毫秒）
             */
            private int timeout = 2000;
            
            /**
             * Redis缓存过期时间（秒）
             */
            private long expireAfterWrite = 3600;
            
            /**
             * Redis连接池最大连接数
             */
            private int maxTotal = 8;
            
            /**
             * Redis连接池最大空闲连接数
             */
            private int maxIdle = 8;
            
            /**
             * Redis连接池最小空闲连接数
             */
            private int minIdle = 0;
        }
    }
    
    @Data
    public static class MultiLevelConfig {
        /**
         * 是否启用多级缓存
         */
        private boolean enabled = true;
        
        /**
         * 多级缓存类型
         */
        private String type = "local-redis";
        
        /**
         * 多级缓存过期时间（秒）
         */
        private long expireAfterWrite = 3600;
        
        /**
         * 多级缓存刷新时间（秒）
         */
        private long refreshAfterWrite = 1800;
        
        /**
         * 多级缓存最大容量
         */
        private int maximumSize = 10000;
    }

    @Data
    public static class WarmupConfig {
        /**
         * 是否启用缓存预热
         */
        private boolean enabled = true;

        /**
         * 预热线程池大小
         */
        private int threadPoolSize = 5;

        /**
         * 预热超时时间（秒）
         */
        private long timeout = 300;

        /**
         * 预热重试次数
         */
        private int retryCount = 3;

        /**
         * 预热重试间隔（秒）
         */
        private long retryInterval = 5;

        /**
         * 预热类型配置
         */
        private Map<String, TypeConfig> types = new HashMap<>();

        @Data
        public static class TypeConfig {
            /**
             * 缓存键模式
             */
            private String keyPattern;

            /**
             * 预热优先级（1-10）
             */
            private int priority = 5;

            /**
             * 预热批次大小
             */
            private int batchSize = 100;

            /**
             * 预热间隔（秒）
             */
            private long interval = 60;
        }
    }

    @Bean
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(local.getMaximumSize())
            .expireAfterWrite(Duration.ofSeconds(local.getExpireAfterWrite())));
        return cacheManager;
    }

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(distributed.getRedis().getExpireAfterWrite()))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .prefixCacheNameWith(distributed.getRedis().getHost());

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // 文档模块缓存配置
        cacheConfigurations.put("documentChunks", config.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("documentContent", config.entryTtl(Duration.ofHours(1)));
        
        // 搜索模块缓存配置
        cacheConfigurations.put("searchResults", config.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("searchSuggestions", config.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("searchStatistics", config.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("userBehavior", config.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("synonymGroups", config.entryTtl(Duration.ofHours(12)));
        cacheConfigurations.put("searchHighlight", config.entryTtl(Duration.ofMinutes(10)));
        
        // 向量模块缓存配置
        cacheConfigurations.put("vectorCache", config.entryTtl(Duration.ofHours(1)));
        
        // 推理模块缓存配置
        cacheConfigurations.put("inferenceResults", config.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("modelCache", config.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
} 