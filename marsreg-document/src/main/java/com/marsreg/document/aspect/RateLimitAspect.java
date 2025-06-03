package com.marsreg.document.aspect;

import com.google.common.cache.LoadingCache;
import com.marsreg.document.annotation.RateLimit;
import com.marsreg.document.config.RateLimitConfig.TokenBucket;
import com.marsreg.document.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {

    private final LoadingCache<String, TokenBucket> tokenBuckets;

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = getKey(joinPoint, rateLimit);
        TokenBucket tokenBucket = tokenBuckets.get(key);
        
        if (!tokenBucket.tryAcquire()) {
            log.warn("请求频率超限: key={}", key);
            throw new RateLimitExceededException("请求频率超限");
        }
        
        return joinPoint.proceed();
    }

    private String getKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        if (!rateLimit.key().isEmpty()) {
            return rateLimit.key();
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getRemoteAddr();
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        
        return methodName + ":" + ip;
    }
} 