package com.marsreg.document.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Configuration
@ConfigurationProperties(prefix = "document.rate-limit")
public class RateLimitConfig {
    private RateLimitProperties upload;
    private RateLimitProperties download;
    private RateLimitProperties defaultConfig;

    @Data
    public static class RateLimitProperties {
        private int limit = 100;
        private int timeWindow = 60;
    }

    @Bean
    public LoadingCache<String, TokenBucket> tokenBuckets() {
        return CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, TokenBucket>() {
                @Override
                public TokenBucket load(String key) {
                    RateLimitProperties config = getConfigForKey(key);
                    return new TokenBucket(config.getLimit(), config.getTimeWindow());
                }
            });
    }

    private RateLimitProperties getConfigForKey(String key) {
        if (key.startsWith("upload:")) {
            return upload;
        } else if (key.startsWith("download:")) {
            return download;
        }
        return defaultConfig;
    }

    public static class TokenBucket {
        private final int capacity;
        private final long refillInterval;
        private final AtomicInteger tokens;
        private long lastRefillTime;

        public TokenBucket(int capacity, int refillInterval) {
            this.capacity = capacity;
            this.refillInterval = refillInterval * 1000L;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = System.currentTimeMillis();
        }

        public boolean tryAcquire() {
            refill();
            return tokens.getAndDecrement() > 0;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;
            int tokensToAdd = (int) (timePassed / refillInterval);
            
            if (tokensToAdd > 0) {
                int currentTokens = tokens.get();
                int newTokens = Math.min(capacity, currentTokens + tokensToAdd);
                tokens.set(newTokens);
                lastRefillTime = now;
            }
        }
    }
} 