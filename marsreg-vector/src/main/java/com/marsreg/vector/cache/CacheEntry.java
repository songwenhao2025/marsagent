package com.marsreg.vector.cache;

import java.time.LocalDateTime;

/**
 * 缓存条目
 */
public class CacheEntry {
    private final String key;
    private final float[] vector;
    private final LocalDateTime expiryTime;
    private int accessCount;
    private long lastAccessTime;
    private final long size;

    public CacheEntry(String key, float[] vector) {
        this.key = key;
        this.vector = vector;
        this.expiryTime = LocalDateTime.now().plusHours(1);
        this.accessCount = 0;
        this.lastAccessTime = System.currentTimeMillis();
        this.size = vector.length * 4; // 每个float占4字节
    }

    public String getKey() {
        return key;
    }

    public float[] getVector() {
        incrementAccessCount();
        return vector;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public void incrementAccessCount() {
        accessCount++;
        lastAccessTime = System.currentTimeMillis();
    }

    public int getAccessCount() {
        return accessCount;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public long getSize() {
        return size;
    }
} 