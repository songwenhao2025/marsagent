package com.marsreg.common.aspect;

import com.marsreg.common.annotation.RateLimit;
import com.marsreg.common.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * 处理 @RateLimit 注解的切面类
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_SCRIPT = 
        "local key = KEYS[1] " +
        "local limit = tonumber(ARGV[1]) " +
        "local current = tonumber(redis.call('get', key) or '0') " +
        "if current + 1 > limit then " +
        "    return 0 " +
        "else " +
        "    redis.call('incrby', key, 1) " +
        "    redis.call('expire', key, ARGV[2]) " +
        "    return 1 " +
        "end";

    private static final DefaultRedisScript<Long> REDIS_SCRIPT = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);

    @Around("@annotation(com.marsreg.common.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return point.proceed();
        }

        String key = getRateLimitKey(point, rateLimit);
        Long result = redisTemplate.execute(
            REDIS_SCRIPT,
            Collections.singletonList(key),
            rateLimit.limit(),
            rateLimit.timeUnit().toSeconds(rateLimit.time())
        );

        if (result == null || result == 0) {
            throw new RateLimitException(rateLimit.message());
        }

        return point.proceed();
    }

    private String getRateLimitKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        StringBuilder key = new StringBuilder("rate_limit:");
        key.append(point.getTarget().getClass().getName())
           .append(":")
           .append(point.getSignature().getName());

        if (rateLimit.key().length() > 0) {
            key.append(":").append(rateLimit.key());
        }

        return key.toString();
    }
} 