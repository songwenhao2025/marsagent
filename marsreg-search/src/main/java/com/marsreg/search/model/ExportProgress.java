package com.marsreg.search.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExportProgress {
    private String taskId;
    private int totalSteps;
    private int completedSteps;
    private String currentStep;
    private double progress;
    private LocalDateTime startTime;
    private LocalDateTime lastUpdateTime;
    private String status;
    private String errorMessage;
    
    public void updateProgress(int completedSteps, String currentStep) {
        this.completedSteps = completedSteps;
        this.currentStep = currentStep;
        this.progress = (double) completedSteps / totalSteps * 100;
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    public void setError(String errorMessage) {
        this.status = "ERROR";
        this.errorMessage = errorMessage;
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    public void setCompleted() {
        this.completedSteps = this.totalSteps;
        this.progress = 100.0;
        this.status = "COMPLETED";
        this.lastUpdateTime = LocalDateTime.now();
    }
} 