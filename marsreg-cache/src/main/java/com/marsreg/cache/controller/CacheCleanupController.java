package com.marsreg.cache.controller;

import com.marsreg.cache.service.CacheCleanupService;
import com.marsreg.cache.service.CacheCleanupService.CleanupStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 缓存清理控制器
 */
@RestController
@RequestMapping("/api/cache/cleanup")
public class CacheCleanupController {

    @Autowired
    private CacheCleanupService cacheCleanupService;

    /**
     * 清理指定类型的缓存
     */
    @PostMapping("/type/{type}")
    public ResponseEntity<Long> cleanupByType(@PathVariable String type) {
        return ResponseEntity.ok(cacheCleanupService.cleanupByType(type));
    }

    /**
     * 清理所有缓存
     */
    @PostMapping("/all")
    public ResponseEntity<Long> cleanupAll() {
        return ResponseEntity.ok(cacheCleanupService.cleanupAll());
    }

    /**
     * 按模式清理缓存
     */
    @PostMapping("/pattern")
    public ResponseEntity<Long> cleanupByPattern(@RequestParam String pattern) {
        return ResponseEntity.ok(cacheCleanupService.cleanupByPattern(pattern));
    }

    /**
     * 清理指定的键列表
     */
    @PostMapping("/keys")
    public ResponseEntity<Long> cleanupByKeys(@RequestBody List<String> keys) {
        return ResponseEntity.ok(cacheCleanupService.cleanupByKeys(keys));
    }

    /**
     * 清理过期的缓存
     */
    @PostMapping("/expired")
    public ResponseEntity<Long> cleanupExpired() {
        return ResponseEntity.ok(cacheCleanupService.cleanupExpired());
    }

    /**
     * 获取清理状态
     */
    @GetMapping("/status/{type}")
    public ResponseEntity<CleanupStatus> getCleanupStatus(@PathVariable String type) {
        return ResponseEntity.ok(cacheCleanupService.getCleanupStatus(type));
    }

    /**
     * 获取总清理状态
     */
    @GetMapping("/status")
    public ResponseEntity<CleanupStatus> getTotalCleanupStatus() {
        return ResponseEntity.ok(cacheCleanupService.getTotalCleanupStatus());
    }
} 