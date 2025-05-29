package com.marsreg.cache.service;

import com.marsreg.cache.service.impl.MultiLevelCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MultiLevelCacheServiceTest {

    @Autowired
    private MultiLevelCacheService cacheService;

    @Autowired
    private CacheManager localCacheManager;

    @Autowired
    private CacheManager redisCacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        cacheService.clear();
    }

    @Test
    void testPutAndGet() {
        // 测试基本存取
        String key = "test:key";
        String value = "test:value";
        
        cacheService.put(key, value);
        String result = cacheService.get(key, String.class);
        
        assertEquals(value, result);
    }

    @Test
    void testGetWithValueLoader() {
        // 测试值加载器
        String key = "test:loader";
        String value = "test:loaded";
        
        Callable<String> valueLoader = () -> value;
        String result = cacheService.get(key, String.class, valueLoader);
        
        assertEquals(value, result);
        
        // 验证缓存是否生效
        String cachedResult = cacheService.get(key, String.class);
        assertEquals(value, cachedResult);
    }

    @Test
    void testEvict() {
        // 测试删除
        String key = "test:evict";
        String value = "test:value";
        
        cacheService.put(key, value);
        cacheService.evict(key);
        
        String result = cacheService.get(key, String.class);
        assertNull(result);
    }

    @Test
    void testClear() {
        // 测试清空
        String key1 = "test:clear:1";
        String key2 = "test:clear:2";
        String value = "test:value";
        
        cacheService.put(key1, value);
        cacheService.put(key2, value);
        cacheService.clear();
        
        assertNull(cacheService.get(key1, String.class));
        assertNull(cacheService.get(key2, String.class));
    }

    @Test
    void testStats() {
        // 测试统计信息
        String key = "test:stats";
        String value = "test:value";
        
        cacheService.put(key, value);
        cacheService.get(key, String.class); // 命中
        cacheService.get("non-existent", String.class); // 未命中
        
        MultiLevelCacheService.CacheStats stats = cacheService.getStats();
        assertTrue(stats.getHitCount() > 0);
        assertTrue(stats.getMissCount() > 0);
    }
} 