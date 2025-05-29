package com.marsreg.common.service.impl;

import com.marsreg.common.config.BackupConfig;
import com.marsreg.common.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final BackupConfig backupConfig;
    private LocalDateTime lastBackupTime;

    @Override
    public String backupDatabase() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = String.format("db_backup_%s.sql", timestamp);
            File backupFile = new File(backupConfig.getBackupDir(), backupFileName);

            // 确保备份目录存在
            backupFile.getParentFile().mkdirs();

            // 构建mysqldump命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                backupConfig.getMysqlBinPath() + "/mysqldump",
                "-u" + backupConfig.getMysqlUsername(),
                "-p" + backupConfig.getMysqlPassword(),
                backupConfig.getMysqlDatabase()
            );
            processBuilder.redirectOutput(backupFile);

            // 执行备份
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                lastBackupTime = LocalDateTime.now();
                log.info("数据库备份成功: {}", backupFile.getAbsolutePath());
                return backupFile.getAbsolutePath();
            } else {
                throw new RuntimeException("数据库备份失败，退出码: " + exitCode);
            }
        } catch (Exception e) {
            log.error("数据库备份失败", e);
            throw new RuntimeException("数据库备份失败: " + e.getMessage());
        }
    }

    @Override
    public String backupFiles(String directory) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = String.format("files_backup_%s.zip", timestamp);
            File backupFile = new File(backupConfig.getBackupDir(), backupFileName);

            // 确保备份目录存在
            backupFile.getParentFile().mkdirs();

            // 创建ZIP文件
            Path sourcePath = Paths.get(directory);
            Path backupPath = backupFile.toPath();
            Files.createDirectories(backupPath.getParent());

            // 使用ProcessBuilder执行zip命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                "zip",
                "-r",
                backupFile.getAbsolutePath(),
                directory
            );
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                lastBackupTime = LocalDateTime.now();
                log.info("文件备份成功: {}", backupFile.getAbsolutePath());
                return backupFile.getAbsolutePath();
            } else {
                throw new RuntimeException("文件备份失败，退出码: " + exitCode);
            }
        } catch (Exception e) {
            log.error("文件备份失败", e);
            throw new RuntimeException("文件备份失败: " + e.getMessage());
        }
    }

    @Override
    public void restoreDatabase(String backupPath) {
        try {
            File backupFile = new File(backupPath);
            if (!backupFile.exists()) {
                throw new RuntimeException("备份文件不存在: " + backupPath);
            }

            // 构建mysql命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                backupConfig.getMysqlBinPath() + "/mysql",
                "-u" + backupConfig.getMysqlUsername(),
                "-p" + backupConfig.getMysqlPassword(),
                backupConfig.getMysqlDatabase()
            );
            processBuilder.redirectInput(backupFile);

            // 执行恢复
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("数据库恢复失败，退出码: " + exitCode);
            }
            log.info("数据库恢复成功");
        } catch (Exception e) {
            log.error("数据库恢复失败", e);
            throw new RuntimeException("数据库恢复失败: " + e.getMessage());
        }
    }

    @Override
    public void restoreFiles(String backupPath, String targetDirectory) {
        try {
            File backupFile = new File(backupPath);
            if (!backupFile.exists()) {
                throw new RuntimeException("备份文件不存在: " + backupPath);
            }

            // 使用ProcessBuilder执行unzip命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                "unzip",
                "-o",
                backupFile.getAbsolutePath(),
                "-d",
                targetDirectory
            );
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("文件恢复失败，退出码: " + exitCode);
            }
            log.info("文件恢复成功");
        } catch (Exception e) {
            log.error("文件恢复失败", e);
            throw new RuntimeException("文件恢复失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> listBackups() {
        File backupDir = new File(backupConfig.getBackupDir());
        if (!backupDir.exists()) {
            return List.of();
        }
        return Arrays.stream(backupDir.listFiles())
                .filter(File::isFile)
                .sorted(Comparator.comparing(File::lastModified).reversed())
                .map(file -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("name", file.getName());
                    info.put("path", file.getAbsolutePath());
                    info.put("size", file.length());
                    info.put("lastModified", LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(file.lastModified()),
                        java.time.ZoneId.systemDefault()
                    ));
                    return info;
                })
                .collect(Collectors.toList());
    }

    @Override
    public int cleanupOldBackups() {
        try {
            File backupDir = new File(backupConfig.getBackupDir());
            if (!backupDir.exists()) {
                return 0;
            }

            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(backupConfig.getRetentionDays());
            long cutoffTimeMillis = cutoffTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000;

            List<File> filesToDelete = Arrays.stream(backupDir.listFiles())
                    .filter(file -> file.isFile() && file.lastModified() < cutoffTimeMillis)
                    .collect(Collectors.toList());

            int deletedCount = 0;
            for (File file : filesToDelete) {
                try {
                    Files.delete(file.toPath());
                    log.info("删除过期备份文件: {}", file.getAbsolutePath());
                    deletedCount++;
                } catch (IOException e) {
                    log.error("删除备份文件失败: {}", file.getAbsolutePath(), e);
                }
            }
            return deletedCount;
        } catch (Exception e) {
            log.error("清理备份文件失败", e);
            throw new RuntimeException("清理备份文件失败: " + e.getMessage());
        }
    }

    @Override
    public LocalDateTime getLastBackupTime() {
        return lastBackupTime;
    }

    @Scheduled(cron = "${marsreg.backup.cron-expression}")
    public void scheduledBackup() {
        if (!backupConfig.isAutoBackup()) {
            return;
        }

        try {
            // 执行数据库备份
            backupDatabase();

            // 清理过期备份
            cleanupOldBackups();
        } catch (Exception e) {
            log.error("定时备份失败", e);
        }
    }
} 