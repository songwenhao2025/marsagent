package com.marsreg.cache.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsreg.cache.service.CacheBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheBackupServiceImpl implements CacheBackupService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, BackupStatus> backupStatuses = new ConcurrentHashMap<>();
    private final String backupDir = "backup";

    @Override
    public long backup(String type) {
        try {
            String pattern = type + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                BackupStatus status = new BackupStatusImpl();
                backupStatuses.put(type, status);
                status.start();
                
                long backupCount = 0;
                long failedCount = 0;
                Map<String, Object> backupData = new HashMap<>();
                
                for (String key : keys) {
                    try {
                        Object value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            backupData.put(key, value);
                            backupCount++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to backup cache for key: " + key, e);
                        failedCount++;
                    }
                }
                
                if (!backupData.isEmpty()) {
                    String backupPath = saveBackup(type, backupData);
                    status.complete(backupCount, failedCount, backupPath);
                    log.info("Backed up {} cache entries for type: {}", backupCount, type);
                    return backupCount;
                }
            }
        } catch (Exception e) {
            log.error("Failed to backup cache for type: " + type, e);
        }
        return 0;
    }

    @Override
    public long backupAll() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                BackupStatus status = new BackupStatusImpl();
                backupStatuses.put("all", status);
                status.start();
                
                long backupCount = 0;
                long failedCount = 0;
                Map<String, Object> backupData = new HashMap<>();
                
                for (String key : keys) {
                    try {
                        Object value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            backupData.put(key, value);
                            backupCount++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to backup cache for key: " + key, e);
                        failedCount++;
                    }
                }
                
                if (!backupData.isEmpty()) {
                    String backupPath = saveBackup("all", backupData);
                    status.complete(backupCount, failedCount, backupPath);
                    log.info("Backed up all cache entries: {}", backupCount);
                    return backupCount;
                }
            }
        } catch (Exception e) {
            log.error("Failed to backup all cache", e);
        }
        return 0;
    }

    @Override
    public long backupByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                BackupStatus status = new BackupStatusImpl();
                backupStatuses.put(pattern, status);
                status.start();
                
                long backupCount = 0;
                long failedCount = 0;
                Map<String, Object> backupData = new HashMap<>();
                
                for (String key : keys) {
                    try {
                        Object value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            backupData.put(key, value);
                            backupCount++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to backup cache for key: " + key, e);
                        failedCount++;
                    }
                }
                
                if (!backupData.isEmpty()) {
                    String backupPath = saveBackup(pattern, backupData);
                    status.complete(backupCount, failedCount, backupPath);
                    log.info("Backed up {} cache entries for pattern: {}", backupCount, pattern);
                    return backupCount;
                }
            }
        } catch (Exception e) {
            log.error("Failed to backup cache for pattern: " + pattern, e);
        }
        return 0;
    }

    @Override
    public long restore(String type) {
        try {
            BackupStatus status = backupStatuses.get(type);
            if (status != null && status.getBackupPath() != null) {
                Map<String, Object> backupData = loadBackup(status.getBackupPath());
                if (backupData != null && !backupData.isEmpty()) {
                    long restoreCount = 0;
                    for (Map.Entry<String, Object> entry : backupData.entrySet()) {
                        try {
                            redisTemplate.opsForValue().set(entry.getKey(), entry.getValue());
                            restoreCount++;
                        } catch (Exception e) {
                            log.error("Failed to restore cache for key: " + entry.getKey(), e);
                        }
                    }
                    log.info("Restored {} cache entries for type: {}", restoreCount, type);
                    return restoreCount;
                }
            }
        } catch (Exception e) {
            log.error("Failed to restore cache for type: " + type, e);
        }
        return 0;
    }

    @Override
    public long restoreAll() {
        try {
            BackupStatus status = backupStatuses.get("all");
            if (status != null && status.getBackupPath() != null) {
                Map<String, Object> backupData = loadBackup(status.getBackupPath());
                if (backupData != null && !backupData.isEmpty()) {
                    long restoreCount = 0;
                    for (Map.Entry<String, Object> entry : backupData.entrySet()) {
                        try {
                            redisTemplate.opsForValue().set(entry.getKey(), entry.getValue());
                            restoreCount++;
                        } catch (Exception e) {
                            log.error("Failed to restore cache for key: " + entry.getKey(), e);
                        }
                    }
                    log.info("Restored all cache entries: {}", restoreCount);
                    return restoreCount;
                }
            }
        } catch (Exception e) {
            log.error("Failed to restore all cache", e);
        }
        return 0;
    }

    @Override
    public long restoreByPattern(String pattern) {
        try {
            BackupStatus status = backupStatuses.get(pattern);
            if (status != null && status.getBackupPath() != null) {
                Map<String, Object> backupData = loadBackup(status.getBackupPath());
                if (backupData != null && !backupData.isEmpty()) {
                    long restoreCount = 0;
                    for (Map.Entry<String, Object> entry : backupData.entrySet()) {
                        try {
                            redisTemplate.opsForValue().set(entry.getKey(), entry.getValue());
                            restoreCount++;
                        } catch (Exception e) {
                            log.error("Failed to restore cache for key: " + entry.getKey(), e);
                        }
                    }
                    log.info("Restored {} cache entries for pattern: {}", restoreCount, pattern);
                    return restoreCount;
                }
            }
        } catch (Exception e) {
            log.error("Failed to restore cache for pattern: " + pattern, e);
        }
        return 0;
    }

    @Override
    public BackupStatus getBackupStatus(String type) {
        return backupStatuses.getOrDefault(type, new BackupStatusImpl());
    }

    @Override
    public BackupStatus getTotalBackupStatus() {
        return backupStatuses.getOrDefault("all", new BackupStatusImpl());
    }

    private String saveBackup(String type, Map<String, Object> data) throws IOException {
        Path backupPath = Paths.get(backupDir, type + "_" + System.currentTimeMillis() + ".json");
        Files.createDirectories(backupPath.getParent());
        objectMapper.writeValue(backupPath.toFile(), data);
        return backupPath.toString();
    }

    private Map<String, Object> loadBackup(String path) throws IOException {
        File backupFile = new File(path);
        if (backupFile.exists()) {
            return objectMapper.readValue(backupFile, Map.class);
        }
        return null;
    }
} 