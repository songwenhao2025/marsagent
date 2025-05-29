package com.marsreg.cache.controller;

import com.marsreg.cache.service.CacheRateLimitService;
import com.marsreg.cache.service.CacheRateLimitService.RateLimitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 缓存限流控制器
 */
@RestController
@RequestMapping("/api/cache/rate-limit")
public class CacheRateLimitController {

    @Autowired
    private CacheRateLimitService cacheRateLimitService;

    /**
     * 尝试获取令牌
     */
    @PostMapping("/{key}/acquire")
    public ResponseEntity<Boolean> tryAcquire(
            @PathVariable String key,
            @RequestParam(defaultValue = "1") int permits) {
        return ResponseEntity.ok(cacheRateLimitService.tryAcquire(key, permits));
    }

    /**
     * 获取限流配置
     */
    @GetMapping("/{key}/config")
    public ResponseEntity<RateLimitConfig> getConfig(@PathVariable String key) {
        return ResponseEntity.ok(cacheRateLimitService.getConfig(key));
    }

    /**
     * 更新限流配置
     */
    @PutMapping("/{key}/config")
    public ResponseEntity<Void> updateConfig(
            @PathVariable String key,
            @RequestBody RateLimitConfig config) {
        cacheRateLimitService.updateConfig(key, config);
        return ResponseEntity.ok().build();
    }

    /**
     * 重置限流配置
     */
    @PostMapping("/{key}/reset")
    public ResponseEntity<Void> resetConfig(@PathVariable String key) {
        cacheRateLimitService.resetConfig(key);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取限流统计信息
     */
    @GetMapping("/{key}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String key) {
        return ResponseEntity.ok(cacheRateLimitService.getStats(key));
    }
} 