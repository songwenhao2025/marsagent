package com.marsreg.cache.service;

import com.marsreg.cache.config.CacheConfig;
import com.marsreg.cache.config.CacheConfig.LocalConfig;
import com.marsreg.cache.config.CacheConfig.DistributedConfig;
import com.marsreg.cache.config.CacheConfig.MultiLevelConfig;

/**
 * 缓存配置服务接口
 */
public interface CacheConfigService {
    
    /**
     * 获取本地缓存配置
     */
    LocalConfig getLocalConfig();
    
    /**
     * 更新本地缓存配置
     */
    void updateLocalConfig(LocalConfig config);
    
    /**
     * 获取分布式缓存配置
     */
    DistributedConfig getDistributedConfig();
    
    /**
     * 更新分布式缓存配置
     */
    void updateDistributedConfig(DistributedConfig config);
    
    /**
     * 获取多级缓存配置
     */
    MultiLevelConfig getMultiLevelConfig();
    
    /**
     * 更新多级缓存配置
     */
    void updateMultiLevelConfig(MultiLevelConfig config);
    
    /**
     * 获取完整缓存配置
     */
    CacheConfig getCacheConfig();
    
    /**
     * 更新完整缓存配置
     */
    void updateCacheConfig(CacheConfig config);
    
    /**
     * 重置缓存配置
     */
    void resetCacheConfig();
} 