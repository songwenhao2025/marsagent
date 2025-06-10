package com.marsreg.vector.cache.eviction;

import com.marsreg.vector.cache.CacheEntry;
import java.util.List;
import java.util.Map;

public interface CacheEvictionStrategy {
    /**
     * 选择要淘汰的缓存项
     * @param entries 所有缓存项
     * @param maxSize 最大缓存大小
     * @return 要淘汰的缓存项列表
     */
    List<String> selectEvictionEntries(Map<String, CacheEntry> entries, int maxSize);
    
    /**
     * 获取策略名称
     */
    String getStrategyName();

    String evict(Map<String, CacheEntry> cache);
} 