package com.marsreg.cache.service;

public interface CacheBackupService {
    
    /**
     * 备份指定类型的缓存
     *
     * @param type 缓存类型
     * @return 备份的缓存数量
     */
    long backup(String type);
    
    /**
     * 备份所有缓存
     *
     * @return 备份的缓存数量
     */
    long backupAll();
    
    /**
     * 备份指定模式的缓存
     *
     * @param pattern 缓存键模式
     * @return 备份的缓存数量
     */
    long backupByPattern(String pattern);
    
    /**
     * 恢复指定类型的缓存
     *
     * @param type 缓存类型
     * @return 恢复的缓存数量
     */
    long restore(String type);
    
    /**
     * 恢复所有缓存
     *
     * @return 恢复的缓存数量
     */
    long restoreAll();
    
    /**
     * 恢复指定模式的缓存
     *
     * @param pattern 缓存键模式
     * @return 恢复的缓存数量
     */
    long restoreByPattern(String pattern);
    
    /**
     * 获取备份状态
     *
     * @param type 缓存类型
     * @return 备份状态
     */
    BackupStatus getBackupStatus(String type);
    
    /**
     * 获取所有备份状态
     *
     * @return 备份状态
     */
    BackupStatus getTotalBackupStatus();
    
    /**
     * 备份状态
     */
    interface BackupStatus {
        /**
         * 获取备份的缓存数量
         */
        long getBackupCount();
        
        /**
         * 获取备份失败的缓存数量
         */
        long getFailedCount();
        
        /**
         * 获取备份开始时间
         */
        long getStartTime();
        
        /**
         * 获取备份结束时间
         */
        long getEndTime();
        
        /**
         * 获取备份耗时
         */
        long getDuration();
        
        /**
         * 获取备份成功率
         */
        double getSuccessRate();
        
        /**
         * 获取备份文件路径
         */
        String getBackupPath();
    }
} 