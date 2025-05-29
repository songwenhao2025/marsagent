package com.marsreg.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "marsreg.backup")
public class BackupConfig {
    private String backupDir;
    private String mysqlBinPath;
    private String mysqlUsername;
    private String mysqlPassword;
    private String mysqlDatabase;
    private int retentionDays = 7;
    private boolean autoBackup = true;
    private String cronExpression = "0 0 2 * * ?"; // 每天凌晨2点
} 