package com.marsreg.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "marsreg.cache")
public class CacheProperties {
    /**
     * 是否启用缓存
     */
    private boolean enabled = true;

    /**
     * 缓存类型：memory/redis
     */
    private String type = "memory";

    /**
     * Redis配置
     */
    private Redis redis = new Redis();

    /**
     * 内存缓存配置
     */
    private Memory memory = new Memory();

    @Data
    public static class Redis {
        /**
         * Redis服务器地址
         */
        private String host = "localhost";

        /**
         * Redis服务器端口
         */
        private int port = 6379;

        /**
         * Redis密码
         */
        private String password;

        /**
         * 数据库索引
         */
        private int database = 0;

        /**
         * 连接超时时间（毫秒）
         */
        private int timeout = 2000;

        /**
         * 连接池最大连接数
         */
        private int maxTotal = 8;

        /**
         * 连接池最大空闲连接数
         */
        private int maxIdle = 8;

        /**
         * 连接池最小空闲连接数
         */
        private int minIdle = 0;
    }

    @Data
    public static class Memory {
        /**
         * 最大缓存条目数
         */
        private int maxSize = 10000;

        /**
         * 默认过期时间（秒）
         */
        private long defaultExpire = 3600;

        /**
         * 是否启用统计
         */
        private boolean enableStats = true;
    }
} 