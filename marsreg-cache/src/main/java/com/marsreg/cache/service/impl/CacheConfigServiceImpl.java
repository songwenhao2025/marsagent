package com.marsreg.cache.service.impl;

import com.marsreg.cache.config.CacheConfig;
import com.marsreg.cache.config.CacheConfig.LocalConfig;
import com.marsreg.cache.config.CacheConfig.DistributedConfig;
import com.marsreg.cache.config.CacheConfig.MultiLevelConfig;
import com.marsreg.cache.service.CacheConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * 缓存配置服务实现
 */
@Slf4j
@Service
public class CacheConfigServiceImpl implements CacheConfigService {

    @Autowired
    private CacheConfig cacheConfig;

    @Override
    public LocalConfig getLocalConfig() {
        return cacheConfig.getLocal();
    }

    @Override
    public void updateLocalConfig(LocalConfig config) {
        cacheConfig.setLocal(config);
        log.info("更新本地缓存配置: {}", config);
    }

    @Override
    public DistributedConfig getDistributedConfig() {
        return cacheConfig.getDistributed();
    }

    @Override
    public void updateDistributedConfig(DistributedConfig config) {
        cacheConfig.setDistributed(config);
        log.info("更新分布式缓存配置: {}", config);
    }

    @Override
    public MultiLevelConfig getMultiLevelConfig() {
        return cacheConfig.getMultiLevel();
    }

    @Override
    public void updateMultiLevelConfig(MultiLevelConfig config) {
        cacheConfig.setMultiLevel(config);
        log.info("更新多级缓存配置: {}", config);
    }

    @Override
    public CacheConfig getCacheConfig() {
        return cacheConfig;
    }

    @Override
    public void updateCacheConfig(CacheConfig config) {
        this.cacheConfig = config;
        log.info("更新完整缓存配置: {}", config);
    }

    @Override
    public void resetCacheConfig() {
        // 重置为默认配置
        CacheConfig defaultConfig = new CacheConfig();
        defaultConfig.setLocal(new LocalConfig());
        defaultConfig.setDistributed(new DistributedConfig());
        defaultConfig.setMultiLevel(new MultiLevelConfig());
        
        this.cacheConfig = defaultConfig;
        log.info("重置缓存配置为默认值");
    }
} 