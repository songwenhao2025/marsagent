package com.marsreg.cache.controller;

import com.marsreg.cache.service.CacheStatsService;
import com.marsreg.cache.service.CacheStatsService.CacheStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 缓存统计控制器
 */
@RestController
@RequestMapping("/api/cache/stats")
public class CacheStatsController {

    @Autowired
    private CacheStatsService cacheStatsService;

    /**
     * 获取指定类型的缓存统计信息
     */
    @GetMapping("/{type}")
    public ResponseEntity<CacheStats> getStats(@PathVariable String type) {
        return ResponseEntity.ok(cacheStatsService.getStats(type));
    }

    /**
     * 获取所有缓存的统计信息
     */
    @GetMapping("/total")
    public ResponseEntity<CacheStats> getTotalStats() {
        return ResponseEntity.ok(cacheStatsService.getTotalStats());
    }

    /**
     * 重置指定类型的缓存统计信息
     */
    @PostMapping("/{type}/reset")
    public ResponseEntity<Void> resetStats(@PathVariable String type) {
        cacheStatsService.resetStats(type);
        return ResponseEntity.ok().build();
    }

    /**
     * 重置所有缓存的统计信息
     */
    @PostMapping("/reset")
    public ResponseEntity<Void> resetAllStats() {
        cacheStatsService.resetAllStats();
        return ResponseEntity.ok().build();
    }

    /**
     * 获取各缓存类型的大小统计
     */
    @GetMapping("/size")
    public ResponseEntity<Map<String, Long>> getSizeStats() {
        return ResponseEntity.ok(cacheStatsService.getSizeStats());
    }

    /**
     * 获取各缓存类型的命中率统计
     */
    @GetMapping("/hit-rate")
    public ResponseEntity<Map<String, Double>> getHitRateStats() {
        return ResponseEntity.ok(cacheStatsService.getHitRateStats());
    }

    /**
     * 获取各缓存类型的加载时间统计
     */
    @GetMapping("/load-time")
    public ResponseEntity<Map<String, Long>> getLoadTimeStats() {
        return ResponseEntity.ok(cacheStatsService.getLoadTimeStats());
    }
} 