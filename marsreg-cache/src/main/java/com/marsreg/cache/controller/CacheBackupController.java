package com.marsreg.cache.controller;

import com.marsreg.cache.service.CacheBackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache/backup")
@RequiredArgsConstructor
public class CacheBackupController {

    private final CacheBackupService backupService;

    @PostMapping("/{type}")
    public ResponseEntity<Map<String, Object>> backup(@PathVariable String type) {
        long count = backupService.backup(type);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cache backed up successfully");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> backupAll() {
        long count = backupService.backupAll();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All cache backed up successfully");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pattern")
    public ResponseEntity<Map<String, Object>> backupByPattern(@RequestParam String pattern) {
        long count = backupService.backupByPattern(pattern);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cache backed up successfully by pattern");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restore/{type}")
    public ResponseEntity<Map<String, Object>> restore(@PathVariable String type) {
        long count = backupService.restore(type);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cache restored successfully");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restore/all")
    public ResponseEntity<Map<String, Object>> restoreAll() {
        long count = backupService.restoreAll();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All cache restored successfully");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restore/pattern")
    public ResponseEntity<Map<String, Object>> restoreByPattern(@RequestParam String pattern) {
        long count = backupService.restoreByPattern(pattern);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cache restored successfully by pattern");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{type}")
    public ResponseEntity<Map<String, Object>> getBackupStatus(@PathVariable String type) {
        CacheBackupService.BackupStatus status = backupService.getBackupStatus(type);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("backupCount", status.getBackupCount());
        response.put("failedCount", status.getFailedCount());
        response.put("startTime", status.getStartTime());
        response.put("endTime", status.getEndTime());
        response.put("duration", status.getDuration());
        response.put("successRate", status.getSuccessRate());
        response.put("backupPath", status.getBackupPath());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTotalBackupStatus() {
        CacheBackupService.BackupStatus status = backupService.getTotalBackupStatus();
        
        Map<String, Object> response = new HashMap<>();
        response.put("backupCount", status.getBackupCount());
        response.put("failedCount", status.getFailedCount());
        response.put("startTime", status.getStartTime());
        response.put("endTime", status.getEndTime());
        response.put("duration", status.getDuration());
        response.put("successRate", status.getSuccessRate());
        response.put("backupPath", status.getBackupPath());
        return ResponseEntity.ok(response);
    }
} 