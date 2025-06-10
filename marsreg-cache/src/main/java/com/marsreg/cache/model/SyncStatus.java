package com.marsreg.cache.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SyncStatus {
    /**
     * 同步状态
     */
    private String status;

    /**
     * 同步进度
     */
    private double progress;

    /**
     * 同步开始时间
     */
    private long startTime;

    /**
     * 同步结束时间
     */
    private long endTime;

    /**
     * 同步错误信息
     */
    private String error;

    /**
     * 同步数据量
     */
    private long dataCount;

    /**
     * 同步成功数据量
     */
    private long successCount;

    /**
     * 同步失败数据量
     */
    private long failCount;
} 