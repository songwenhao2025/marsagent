package com.marsreg.cache.controller;

import com.marsreg.cache.config.CacheConfig;
import com.marsreg.cache.config.CacheConfig.LocalConfig;
import com.marsreg.cache.config.CacheConfig.DistributedConfig;
import com.marsreg.cache.config.CacheConfig.MultiLevelConfig;
import com.marsreg.cache.service.CacheConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 缓存配置控制器
 */
@RestController
@RequestMapping("/api/cache/config")
public class CacheConfigController {

    @Autowired
    private CacheConfigService cacheConfigService;

    /**
     * 获取本地缓存配置
     */
    @GetMapping("/local")
    public ResponseEntity<LocalConfig> getLocalConfig() {
        return ResponseEntity.ok(cacheConfigService.getLocalConfig());
    }

    /**
     * 更新本地缓存配置
     */
    @PutMapping("/local")
    public ResponseEntity<Void> updateLocalConfig(@RequestBody LocalConfig config) {
        cacheConfigService.updateLocalConfig(config);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取分布式缓存配置
     */
    @GetMapping("/distributed")
    public ResponseEntity<DistributedConfig> getDistributedConfig() {
        return ResponseEntity.ok(cacheConfigService.getDistributedConfig());
    }

    /**
     * 更新分布式缓存配置
     */
    @PutMapping("/distributed")
    public ResponseEntity<Void> updateDistributedConfig(@RequestBody DistributedConfig config) {
        cacheConfigService.updateDistributedConfig(config);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取多级缓存配置
     */
    @GetMapping("/multi-level")
    public ResponseEntity<MultiLevelConfig> getMultiLevelConfig() {
        return ResponseEntity.ok(cacheConfigService.getMultiLevelConfig());
    }

    /**
     * 更新多级缓存配置
     */
    @PutMapping("/multi-level")
    public ResponseEntity<Void> updateMultiLevelConfig(@RequestBody MultiLevelConfig config) {
        cacheConfigService.updateMultiLevelConfig(config);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取完整缓存配置
     */
    @GetMapping
    public ResponseEntity<CacheConfig> getCacheConfig() {
        return ResponseEntity.ok(cacheConfigService.getCacheConfig());
    }

    /**
     * 更新完整缓存配置
     */
    @PutMapping
    public ResponseEntity<Void> updateCacheConfig(@RequestBody CacheConfig config) {
        cacheConfigService.updateCacheConfig(config);
        return ResponseEntity.ok().build();
    }

    /**
     * 重置缓存配置
     */
    @PostMapping("/reset")
    public ResponseEntity<Void> resetCacheConfig() {
        cacheConfigService.resetCacheConfig();
        return ResponseEntity.ok().build();
    }
} 