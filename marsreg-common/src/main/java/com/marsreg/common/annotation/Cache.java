package com.marsreg.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存注解
 * 用于方法结果的缓存，支持过期时间和条件缓存
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cache {
    /**
     * 缓存名称
     */
    String name() default "";

    /**
     * 缓存key，支持SpEL表达式
     */
    String key() default "";

    /**
     * 缓存过期时间
     */
    long expire() default -1;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否缓存null值
     */
    boolean cacheNull() default false;

    /**
     * 缓存条件，支持SpEL表达式
     */
    String condition() default "";

    /**
     * 是否异步加载
     */
    boolean async() default false;

    /**
     * 是否使用本地缓存
     */
    boolean local() default false;
} 