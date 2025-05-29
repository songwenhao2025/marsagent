package com.marsreg.common.annotation;

import java.lang.annotation.*;

/**
 * 日志注解
 * 用于记录方法的调用日志，包括入参、出参、执行时间等信息
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 操作类型
     */
    String operation() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 是否保存请求参数
     */
    boolean saveRequestData() default true;

    /**
     * 是否保存响应结果
     */
    boolean saveResponseData() default true;

    /**
     * 是否记录执行时间
     */
    boolean saveExecutionTime() default true;

    /**
     * 是否记录异常信息
     */
    boolean saveException() default true;
} 