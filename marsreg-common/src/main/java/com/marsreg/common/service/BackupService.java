package com.marsreg.common.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface BackupService {
    /**
     * 执行数据库备份
     * @return 备份文件路径
     */
    String backupDatabase();

    /**
     * 执行文件备份
     * @param directory 源目录
     * @return 备份文件路径
     */
    String backupFiles(String directory);

    /**
     * 从备份文件恢复数据库
     * @param backupPath 备份文件路径
     */
    void restoreDatabase(String backupPath);

    /**
     * 从备份文件恢复文件
     * @param backupPath 备份文件路径
     * @param targetDirectory 目标目录
     */
    void restoreFiles(String backupPath, String targetDirectory);

    /**
     * 获取所有备份文件信息
     * @return 备份文件信息列表
     */
    List<Map<String, Object>> listBackups();

    /**
     * 清理过期备份
     * @return 删除的备份文件数量
     */
    int cleanupOldBackups();

    /**
     * 获取备份状态
     * @return 最后一次备份的时间
     */
    LocalDateTime getLastBackupTime();
} 