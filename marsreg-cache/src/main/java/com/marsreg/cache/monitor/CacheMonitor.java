package com.marsreg.cache.monitor;

import com.marsreg.cache.service.MultiLevelCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheMonitor {

    private final MultiLevelCacheService cacheService;

    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void monitorCache() {
        MultiLevelCacheService.CacheStats stats = cacheService.getStats();
        
        log.info("Cache Statistics:");
        log.info("Hit Count: {}", stats.getHitCount());
        log.info("Miss Count: {}", stats.getMissCount());
        log.info("Load Success Count: {}", stats.getLoadSuccessCount());
        log.info("Load Failure Count: {}", stats.getLoadFailureCount());
        log.info("Total Load Time: {} ms", stats.getTotalLoadTime());
        log.info("Eviction Count: {}", stats.getEvictionCount());
        
        // 计算命中率
        long totalRequests = stats.getHitCount() + stats.getMissCount();
        if (totalRequests > 0) {
            double hitRate = (double) stats.getHitCount() / totalRequests * 100;
            log.info("Hit Rate: {:.2f}%", hitRate);
        }
    }
} 