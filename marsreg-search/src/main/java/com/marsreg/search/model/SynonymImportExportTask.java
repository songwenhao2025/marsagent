package com.marsreg.search.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SynonymImportExportTask {
    private String taskId;
    private String userId;
    private TaskType type;
    private TaskStatus status;
    private String format;
    private String category;
    private String filePath;
    private int totalCount;
    private int processedCount;
    private int successCount;
    private int failureCount;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    public enum TaskType {
        IMPORT,
        EXPORT
    }
    
    public enum TaskStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    public double getProgress() {
        if (totalCount == 0) return 0.0;
        return (double) processedCount / totalCount * 100;
    }
    
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.CANCELLED;
    }
} 