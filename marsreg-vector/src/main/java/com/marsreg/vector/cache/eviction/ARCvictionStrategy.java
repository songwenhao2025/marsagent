package com.marsreg.vector.cache.eviction;

import com.marsreg.vector.cache.CacheEntry;
import java.util.*;
import java.util.stream.Collectors;

public class ARCvictionStrategy implements CacheEvictionStrategy {
    
    private final Map<String, Integer> t1 = new LinkedHashMap<>(); // 最近访问一次
    private final Map<String, Integer> t2 = new LinkedHashMap<>(); // 最近访问多次
    private final Map<String, Integer> b1 = new LinkedHashMap<>(); // 最近淘汰一次
    private final Map<String, Integer> b2 = new LinkedHashMap<>(); // 最近淘汰多次
    private int p = 0; // 自适应参数
    
    @Override
    public List<String> selectEvictionEntries(Map<String, CacheEntry> entries, int maxSize) {
        List<String> evictionEntries = new ArrayList<>();
        
        // 计算目标大小
        int targetSize = entries.size() - maxSize + 1;
        if (targetSize <= 0) {
            return evictionEntries;
        }
        
        // 根据访问频率和最近访问时间选择要淘汰的项
        while (evictionEntries.size() < targetSize) {
            if (!t1.isEmpty() && (t1.size() > p || (t2.isEmpty() && !b1.isEmpty()))) {
                // 从t1中淘汰
                String key = t1.keySet().iterator().next();
                t1.remove(key);
                b1.put(key, 1);
                evictionEntries.add(key);
            } else if (!t2.isEmpty()) {
                // 从t2中淘汰
                String key = t2.keySet().iterator().next();
                t2.remove(key);
                b2.put(key, 1);
                evictionEntries.add(key);
            } else {
                break;
            }
        }
        
        return evictionEntries;
    }
    
    @Override
    public String getStrategyName() {
        return "ARC";
    }
    
    public void recordAccess(String key) {
        if (t1.containsKey(key)) {
            t1.remove(key);
            t2.put(key, 1);
        } else if (t2.containsKey(key)) {
            t2.remove(key);
            t2.put(key, 1);
        } else if (b1.containsKey(key)) {
            // 调整p
            p = Math.min(p + 1, t1.size() + t2.size());
            b1.remove(key);
            t2.put(key, 1);
        } else if (b2.containsKey(key)) {
            // 调整p
            p = Math.max(p - 1, 0);
            b2.remove(key);
            t2.put(key, 1);
        } else {
            t1.put(key, 1);
        }
    }

    @Override
    public String evict(Map<String, CacheEntry> cache) {
        // 简单实现，实际可根据ARC算法优化
        return cache.keySet().stream().findFirst().orElse(null);
    }
} 