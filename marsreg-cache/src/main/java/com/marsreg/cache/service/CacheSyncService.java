package com.marsreg.cache.service;

import com.marsreg.cache.model.SyncStatus;

public interface CacheSyncService {
    /**
     * 同步本地缓存到分布式缓存
     */
    SyncStatus syncLocalToDistributed();

    /**
     * 同步分布式缓存到本地缓存
     */
    SyncStatus syncDistributedToLocal();

    /**
     * 同步多级缓存
     */
    SyncStatus syncMultiLevel();

    /**
     * 获取同步状态
     */
    SyncStatus getSyncStatus();

    /**
     * 重置同步状态
     */
    void resetSyncStatus();

    /**
     * 暂停同步
     */
    void pauseSync();

    /**
     * 恢复同步
     */
    void resumeSync();

    /**
     * 停止同步
     */
    void stopSync();

    /**
     * 启动同步
     */
    void startSync();
} 