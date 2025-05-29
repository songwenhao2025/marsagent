package com.marsreg.cache.controller;

import com.marsreg.cache.service.CacheWarmupService;
import com.marsreg.cache.service.CacheWarmupService.WarmupResult;
import com.marsreg.cache.service.CacheWarmupService.WarmupStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 缓存预热控制器
 */
@RestController
@RequestMapping("/api/cache/warmup")
public class CacheWarmupController {

    @Autowired
    private CacheWarmupService cacheWarmupService;

    /**
     * 按类型预热缓存
     */
    @PostMapping("/type/{type}")
    public ResponseEntity<WarmupResult> warmupByType(@PathVariable String type) {
        return ResponseEntity.ok(cacheWarmupService.warmupByType(type));
    }

    /**
     * 预热所有缓存
     */
    @PostMapping("/all")
    public ResponseEntity<WarmupResult> warmupAll() {
        return ResponseEntity.ok(cacheWarmupService.warmupAll());
    }

    /**
     * 按模式预热缓存
     */
    @PostMapping("/pattern")
    public ResponseEntity<WarmupResult> warmupByPattern(@RequestParam String pattern) {
        return ResponseEntity.ok(cacheWarmupService.warmupByPattern(pattern));
    }

    /**
     * 从数据源预热缓存
     */
    @PostMapping("/data/{type}")
    public ResponseEntity<WarmupResult> warmupFromData(
            @PathVariable String type,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(cacheWarmupService.warmupFromData(type, data));
    }

    /**
     * 从键列表预热缓存
     */
    @PostMapping("/keys/{type}")
    public ResponseEntity<WarmupResult> warmupFromKeys(
            @PathVariable String type,
            @RequestBody List<String> keys) {
        return ResponseEntity.ok(cacheWarmupService.warmupFromKeys(type, keys));
    }

    /**
     * 获取预热状态
     */
    @GetMapping("/status/{type}")
    public ResponseEntity<WarmupStatus> getWarmupStatus(@PathVariable String type) {
        return ResponseEntity.ok(cacheWarmupService.getWarmupStatus(type));
    }

    /**
     * 获取总体预热状态
     */
    @GetMapping("/status")
    public ResponseEntity<WarmupStatus> getTotalWarmupStatus() {
        return ResponseEntity.ok(cacheWarmupService.getTotalWarmupStatus());
    }
} 