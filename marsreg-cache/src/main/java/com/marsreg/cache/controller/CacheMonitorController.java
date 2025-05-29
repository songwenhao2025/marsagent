package com.marsreg.cache.controller;

import com.marsreg.cache.service.CacheMonitorService;
import com.marsreg.cache.service.CacheMonitorService.CacheStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 缓存监控控制器
 */
@RestController
@RequestMapping("/api/cache/monitor")
public class CacheMonitorController {

    @Autowired
    private CacheMonitorService cacheMonitorService;

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<CacheStats> getCacheStats() {
        return ResponseEntity.ok(cacheMonitorService.getCacheStats());
    }

    /**
     * 获取缓存类型统计信息
     */
    @GetMapping("/stats/type/{type}")
    public ResponseEntity<CacheStats> getCacheStatsByType(@PathVariable String type) {
        return ResponseEntity.ok(cacheMonitorService.getCacheStatsByType(type));
    }

    /**
     * 获取缓存键统计信息
     */
    @GetMapping("/stats/key/{key}")
    public ResponseEntity<CacheStats> getCacheStatsByKey(@PathVariable String key) {
        return ResponseEntity.ok(cacheMonitorService.getCacheStatsByKey(key));
    }

    /**
     * 获取缓存类型列表
     */
    @GetMapping("/types")
    public ResponseEntity<List<String>> getCacheTypes() {
        return ResponseEntity.ok(cacheMonitorService.getCacheTypes());
    }

    /**
     * 获取缓存键列表
     */
    @GetMapping("/keys/{type}")
    public ResponseEntity<List<String>> getCacheKeys(@PathVariable String type) {
        return ResponseEntity.ok(cacheMonitorService.getCacheKeys(type));
    }

    /**
     * 获取缓存值
     */
    @GetMapping("/value/{key}")
    public ResponseEntity<Object> getCacheValue(@PathVariable String key) {
        return ResponseEntity.ok(cacheMonitorService.getCacheValue(key));
    }

    /**
     * 获取缓存过期时间
     */
    @GetMapping("/expire/{key}")
    public ResponseEntity<Long> getCacheExpire(@PathVariable String key) {
        return ResponseEntity.ok(cacheMonitorService.getCacheExpire(key));
    }

    /**
     * 获取缓存大小
     */
    @GetMapping("/size/{type}")
    public ResponseEntity<Long> getCacheSize(@PathVariable String type) {
        return ResponseEntity.ok(cacheMonitorService.getCacheSize(type));
    }

    /**
     * 获取缓存命中率
     */
    @GetMapping("/hit-rate/{type}")
    public ResponseEntity<Double> getCacheHitRate(@PathVariable String type) {
        return ResponseEntity.ok(cacheMonitorService.getCacheHitRate(type));
    }

    /**
     * 获取缓存内存使用情况
     */
    @GetMapping("/memory")
    public ResponseEntity<Map<String, Long>> getCacheMemoryUsage() {
        return ResponseEntity.ok(cacheMonitorService.getCacheMemoryUsage());
    }

    /**
     * 获取缓存健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        return ResponseEntity.ok(cacheMonitorService.getHealthStatus());
    }

    /**
     * 获取缓存性能指标
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        return ResponseEntity.ok(cacheMonitorService.getPerformanceMetrics());
    }

    /**
     * 获取缓存容量使用情况
     */
    @GetMapping("/capacity")
    public ResponseEntity<Map<String, Object>> getCapacityUsage() {
        return ResponseEntity.ok(cacheMonitorService.getCapacityUsage());
    }

    /**
     * 获取缓存错误统计
     */
    @GetMapping("/errors")
    public ResponseEntity<Map<String, Object>> getErrorStats() {
        return ResponseEntity.ok(cacheMonitorService.getErrorStats());
    }

    /**
     * 获取缓存告警信息
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts() {
        return ResponseEntity.ok(cacheMonitorService.getAlerts());
    }

    /**
     * 获取缓存监控配置
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getMonitorConfig() {
        return ResponseEntity.ok(cacheMonitorService.getMonitorConfig());
    }

    /**
     * 更新缓存监控配置
     */
    @PutMapping("/config")
    public ResponseEntity<Void> updateMonitorConfig(@RequestBody Map<String, Object> config) {
        cacheMonitorService.updateMonitorConfig(config);
        return ResponseEntity.ok().build();
    }

    /**
     * 重置缓存监控数据
     */
    @PostMapping("/reset")
    public ResponseEntity<Void> resetMonitorData() {
        cacheMonitorService.resetMonitorData();
        return ResponseEntity.ok().build();
    }
} 