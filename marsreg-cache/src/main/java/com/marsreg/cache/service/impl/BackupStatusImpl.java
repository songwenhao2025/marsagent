package com.marsreg.cache.service.impl;

import com.marsreg.cache.service.CacheBackupService.BackupStatus;
import lombok.Data;

@Data
public class BackupStatusImpl implements BackupStatus {
    private long backupCount;
    private long failedCount;
    private long startTime;
    private long endTime;
    private String backupPath;

    @Override
    public long getDuration() {
        return endTime > 0 ? endTime - startTime : 0;
    }

    @Override
    public double getSuccessRate() {
        long total = backupCount + failedCount;
        return total > 0 ? (double) backupCount / total * 100 : 0;
    }

    @Override
    public BackupStatus start() {
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.backupCount = 0;
        this.failedCount = 0;
        return this;
    }

    @Override
    public BackupStatus complete(long backupCount, long failedCount, String backupPath) {
        this.endTime = System.currentTimeMillis();
        this.backupCount = backupCount;
        this.failedCount = failedCount;
        this.backupPath = backupPath;
        return this;
    }
} 