package com.marsreg.common.controller;

import com.marsreg.common.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@Tag(name = "备份管理", description = "数据库和文件备份管理接口")
@SecurityRequirement(name = "JWT")
public class BackupController {

    private final BackupService backupService;

    @PostMapping("/database")
    @Operation(summary = "创建数据库备份", description = "创建当前数据库的完整备份")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "备份成功"),
        @ApiResponse(responseCode = "500", description = "备份失败")
    })
    public ResponseEntity<Map<String, Object>> backupDatabase() {
        String backupPath = backupService.backupDatabase();
        return ResponseEntity.ok(Map.of("message", "数据库备份成功", "path", backupPath));
    }

    @PostMapping("/files")
    @Operation(summary = "创建文件备份", description = "创建指定目录的文件备份")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "备份成功"),
        @ApiResponse(responseCode = "500", description = "备份失败")
    })
    public ResponseEntity<Map<String, Object>> backupFiles(
            @Parameter(description = "要备份的目录路径") @RequestParam String directory) {
        String backupPath = backupService.backupFiles(directory);
        return ResponseEntity.ok(Map.of("message", "文件备份成功", "path", backupPath));
    }

    @PostMapping("/restore/database")
    @Operation(summary = "恢复数据库备份", description = "从指定的备份文件恢复数据库")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "恢复成功"),
        @ApiResponse(responseCode = "500", description = "恢复失败")
    })
    public ResponseEntity<Map<String, Object>> restoreDatabase(
            @Parameter(description = "备份文件路径") @RequestParam String backupPath) {
        backupService.restoreDatabase(backupPath);
        return ResponseEntity.ok(Map.of("message", "数据库恢复成功"));
    }

    @PostMapping("/restore/files")
    @Operation(summary = "恢复文件备份", description = "从指定的备份文件恢复文件")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "恢复成功"),
        @ApiResponse(responseCode = "500", description = "恢复失败")
    })
    public ResponseEntity<Map<String, Object>> restoreFiles(
            @Parameter(description = "备份文件路径") @RequestParam String backupPath,
            @Parameter(description = "恢复目标目录") @RequestParam String targetDirectory) {
        backupService.restoreFiles(backupPath, targetDirectory);
        return ResponseEntity.ok(Map.of("message", "文件恢复成功"));
    }

    @GetMapping("/list")
    @Operation(summary = "列出所有备份", description = "获取所有可用的备份文件列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    public ResponseEntity<List<Map<String, Object>>> listBackups() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    @DeleteMapping("/cleanup")
    @Operation(summary = "清理旧备份", description = "删除超过保留期限的备份文件")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "清理成功"),
        @ApiResponse(responseCode = "500", description = "清理失败")
    })
    public ResponseEntity<Map<String, Object>> cleanupOldBackups() {
        int count = backupService.cleanupOldBackups();
        return ResponseEntity.ok(Map.of("message", "清理成功", "deletedCount", count));
    }
} 