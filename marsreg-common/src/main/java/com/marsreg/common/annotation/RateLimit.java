package com.marsreg.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 * 用于方法级别的限流控制，支持多种限流策略
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 限流key，支持SpEL表达式
     */
    String key() default "";

    /**
     * 限流阈值
     */
    int limit() default 100;

    /**
     * 时间窗口
     */
    int time() default 60;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 限流策略
     */
    Strategy strategy() default Strategy.COUNTER;

    /**
     * 限流类型
     */
    Type type() default Type.IP;

    /**
     * 限流提示信息
     */
    String message() default "请求太频繁，请稍后再试";

    /**
     * 限流策略枚举
     */
    enum Strategy {
        /**
         * 计数器限流
         */
        COUNTER,
        /**
         * 令牌桶限流
         */
        TOKEN_BUCKET,
        /**
         * 漏桶限流
         */
        LEAKY_BUCKET
    }

    /**
     * 限流类型枚举
     */
    enum Type {
        /**
         * IP限流
         */
        IP,
        /**
         * 用户限流
         */
        USER,
        /**
         * 接口限流
         */
        API,
        /**
         * 自定义限流
         */
        CUSTOM
    }
} 