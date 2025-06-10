package com.marsreg.vector.cache.eviction;

import com.marsreg.vector.cache.CacheEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LFUEvictionStrategy implements CacheEvictionStrategy {
    
    @Override
    public List<String> selectEvictionEntries(Map<String, CacheEntry> entries, int maxSize) {
        return entries.entrySet().stream()
            .sorted(Map.Entry.comparingByValue((e1, e2) -> 
                Integer.compare(e1.getAccessCount(), e2.getAccessCount())))
            .limit(entries.size() - maxSize + 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    @Override
    public String getStrategyName() {
        return "LFU";
    }

    @Override
    public String evict(Map<String, CacheEntry> cache) {
        // 最少使用淘汰策略
        return cache.entrySet().stream()
                .min((e1, e2) -> Integer.compare(e1.getValue().getAccessCount(), e2.getValue().getAccessCount()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
} 