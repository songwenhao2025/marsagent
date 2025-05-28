package com.marsreg.document.entity;

public enum DocumentStatus {
    /**
     * 待处理
     */
    PENDING,

    /**
     * 处理中
     */
    PROCESSING,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 处理失败
     */
    FAILED
} 