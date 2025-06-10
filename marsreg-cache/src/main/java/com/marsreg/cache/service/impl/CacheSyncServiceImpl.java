package com.marsreg.cache.service.impl;

import com.marsreg.cache.service.CacheSyncService;
import com.marsreg.cache.model.SyncStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CacheSyncServiceImpl implements CacheSyncService {

    @Override
    public SyncStatus syncLocalToDistributed() {
        log.info("同步本地缓存到分布式缓存");
        return SyncStatus.builder().status("SUCCESS").build();
    }

    @Override
    public SyncStatus syncDistributedToLocal() {
        log.info("同步分布式缓存到本地缓存");
        return SyncStatus.builder().status("SUCCESS").build();
    }

    @Override
    public SyncStatus syncMultiLevel() {
        log.info("同步多级缓存");
        return SyncStatus.builder().status("SUCCESS").build();
    }

    @Override
    public SyncStatus getSyncStatus() {
        log.info("获取同步状态");
        return SyncStatus.builder().status("IDLE").build();
    }

    @Override
    public void resetSyncStatus() {
        log.info("重置同步状态");
    }

    @Override
    public void pauseSync() {
        log.info("暂停同步");
    }

    @Override
    public void resumeSync() {
        log.info("恢复同步");
    }

    @Override
    public void stopSync() {
        log.info("停止同步");
    }

    @Override
    public void startSync() {
        log.info("启动同步");
    }
} 