package com.marsreg.vector.cache.eviction;

import com.marsreg.vector.cache.CacheEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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

    @Override
    public String evict(Map<String, CacheEntry> cache) {
        if (cache.isEmpty()) {
            return null;
        }

        return cache.entrySet().stream()
            .min(Comparator.comparingLong(e -> e.getValue().getLastAccessTime()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    public String selectKeyToEvict(Map<String, CacheEntry> cache) {
        return cache.entrySet().stream()
                .min(Comparator.comparingLong(entry -> 
                    entry.getValue().getAccessCount()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
} 