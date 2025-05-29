package com.marsreg.common.aspect;

import com.marsreg.common.annotation.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 缓存切面
 * 处理 @Cache 注解的切面类
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(com.marsreg.common.annotation.Cache)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Cache cacheAnnotation = method.getAnnotation(Cache.class);

        if (cacheAnnotation == null) {
            return point.proceed();
        }

        // 解析缓存key
        String cacheKey = parseCacheKey(point, cacheAnnotation);
        if (cacheKey == null) {
            return point.proceed();
        }

        // 检查缓存条件
        if (!checkCacheCondition(point, cacheAnnotation)) {
            return point.proceed();
        }

        // 从缓存中获取数据
        Object result = redisTemplate.opsForValue().get(cacheKey);
        if (result != null) {
            return result;
        }

        // 执行方法
        result = point.proceed();

        // 缓存结果
        if (result != null || cacheAnnotation.cacheNull()) {
            if (cacheAnnotation.expire() > 0) {
                redisTemplate.opsForValue().set(
                    cacheKey,
                    result,
                    cacheAnnotation.expire(),
                    cacheAnnotation.timeUnit()
                );
            } else {
                redisTemplate.opsForValue().set(cacheKey, result);
            }
        }

        return result;
    }

    private String parseCacheKey(ProceedingJoinPoint point, Cache cacheAnnotation) {
        try {
            String key = cacheAnnotation.key();
            if (key.isEmpty()) {
                return cacheAnnotation.name();
            }

            Expression expression = parser.parseExpression(key);
            EvaluationContext context = new StandardEvaluationContext(point.getArgs());
            Object value = expression.getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("解析缓存key失败", e);
            return null;
        }
    }

    private boolean checkCacheCondition(ProceedingJoinPoint point, Cache cacheAnnotation) {
        try {
            String condition = cacheAnnotation.condition();
            if (condition.isEmpty()) {
                return true;
            }

            Expression expression = parser.parseExpression(condition);
            EvaluationContext context = new StandardEvaluationContext(point.getArgs());
            Boolean value = expression.getValue(context, Boolean.class);
            return value != null && value;
        } catch (Exception e) {
            log.error("检查缓存条件失败", e);
            return false;
        }
    }
} 