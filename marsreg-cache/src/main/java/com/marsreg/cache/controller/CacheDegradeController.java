package com.marsreg.cache.controller;

import com.marsreg.cache.service.CacheDegradeService;
import com.marsreg.cache.service.CacheDegradeService.DegradeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 缓存降级控制器
 */
@RestController
@RequestMapping("/api/cache/degrade")
public class CacheDegradeController {

    @Autowired
    private CacheDegradeService cacheDegradeService;

    /**
     * 检查是否允许降级
     */
    @GetMapping("/{key}/check")
    public ResponseEntity<Boolean> isDegradeAllowed(@PathVariable String key) {
        return ResponseEntity.ok(cacheDegradeService.isDegradeAllowed(key));
    }

    /**
     * 获取降级配置
     */
    @GetMapping("/{key}/config")
    public ResponseEntity<DegradeConfig> getConfig(@PathVariable String key) {
        return ResponseEntity.ok(cacheDegradeService.getConfig(key));
    }

    /**
     * 更新降级配置
     */
    @PutMapping("/{key}/config")
    public ResponseEntity<Void> updateConfig(
            @PathVariable String key,
            @RequestBody DegradeConfig config) {
        cacheDegradeService.updateConfig(key, config);
        return ResponseEntity.ok().build();
    }

    /**
     * 重置降级配置
     */
    @PostMapping("/{key}/reset")
    public ResponseEntity<Void> resetConfig(@PathVariable String key) {
        cacheDegradeService.resetConfig(key);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取降级统计信息
     */
    @GetMapping("/{key}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String key) {
        return ResponseEntity.ok(cacheDegradeService.getStats(key));
    }
} 