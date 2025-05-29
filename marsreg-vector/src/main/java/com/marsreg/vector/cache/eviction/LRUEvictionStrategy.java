package com.marsreg.vector.cache.eviction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LRUEvictionStrategy implements CacheEvictionStrategy {
    
    @Override
    public List<String> selectEvictionEntries(Map<String, CacheEntry> entries, int maxSize) {
        return entries.entrySet().stream()
            .sorted(Map.Entry.comparingByValue((e1, e2) -> 
                Long.compare(e1.getLastAccessTime(), e2.getLastAccessTime())))
            .limit(entries.size() - maxSize + 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    @Override
    public String getStrategyName() {
        return "LRU";
    }
} 