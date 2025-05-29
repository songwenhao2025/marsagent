package com.marsreg.cache.controller;

import com.marsreg.cache.service.CacheEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 缓存事件控制器
 */
@RestController
@RequestMapping("/api/cache/events")
public class CacheEventController {

    @Autowired
    private CacheEventService cacheEventService;

    /**
     * 发布缓存事件
     */
    @PostMapping("/{type}")
    public ResponseEntity<Void> publishEvent(
            @PathVariable String type,
            @RequestBody Map<String, Object> data) {
        cacheEventService.publishEvent(type, data);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取事件历史记录
     */
    @GetMapping("/{type}/history")
    public ResponseEntity<List<Map<String, Object>>> getEventHistory(
            @PathVariable String type,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(cacheEventService.getEventHistory(type, limit));
    }

    /**
     * 清除事件历史记录
     */
    @DeleteMapping("/{type}/history")
    public ResponseEntity<Void> clearEventHistory(@PathVariable String type) {
        cacheEventService.clearEventHistory(type);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取所有事件类型
     */
    @GetMapping("/types")
    public ResponseEntity<List<String>> getEventTypes() {
        return ResponseEntity.ok(cacheEventService.getEventTypes());
    }

    /**
     * 获取事件统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getEventStats() {
        return ResponseEntity.ok(cacheEventService.getEventStats());
    }
} 