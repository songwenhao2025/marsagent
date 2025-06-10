package com.marsreg.search.controller;

import com.marsreg.search.model.ExportProgress;
import com.marsreg.search.service.DataExportService;
import com.marsreg.search.service.ExportTaskService;
import com.marsreg.search.service.ExportTaskService.SchedulingStrategy;
import com.marsreg.search.service.ExportTaskService.ExportFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/search/export")
@RequiredArgsConstructor
public class DataExportController {

    private final DataExportService dataExportService;
    private final ExportTaskService exportTaskService;

    @PostMapping("/search-statistics")
    public ResponseEntity<Map<String, String>> exportSearchStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "CSV") DataExportService.ExportFormat format) {
        ExportTaskService.ExportTaskParams params = new ExportTaskService.ExportTaskParams();
        params.setStartTime(startTime);
        params.setEndTime(endTime);
        params.setFormat(format);
        
        String taskId = exportTaskService.createTask(ExportTaskService.ExportTaskType.SEARCH_STATISTICS, params);
        return ResponseEntity.ok(Map.of("taskId", taskId));
    }

    @PostMapping("/user-behavior")
    public ResponseEntity<Map<String, String>> exportUserBehaviorStats(
            @RequestParam List<String> userIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "CSV") DataExportService.ExportFormat format) {
        ExportTaskService.ExportTaskParams params = new ExportTaskService.ExportTaskParams();
        params.setUserIds(userIds);
        params.setStartTime(startTime);
        params.setEndTime(endTime);
        params.setFormat(format);
        
        String taskId = exportTaskService.createTask(ExportTaskService.ExportTaskType.USER_BEHAVIOR, params);
        return ResponseEntity.ok(Map.of("taskId", taskId));
    }

    @PostMapping("/performance-metrics")
    public ResponseEntity<Map<String, String>> exportPerformanceMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "CSV") DataExportService.ExportFormat format) {
        ExportTaskService.ExportTaskParams params = new ExportTaskService.ExportTaskParams();
        params.setStartTime(startTime);
        params.setEndTime(endTime);
        params.setFormat(format);
        
        String taskId = exportTaskService.createTask(ExportTaskService.ExportTaskType.PERFORMANCE_METRICS, params);
        return ResponseEntity.ok(Map.of("taskId", taskId));
    }

    @PostMapping("/hot-keywords")
    public ResponseEntity<Map<String, String>> exportHotKeywords(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "CSV") DataExportService.ExportFormat format) {
        ExportTaskService.ExportTaskParams params = new ExportTaskService.ExportTaskParams();
        params.setSize(size);
        params.setFormat(format);
        
        String taskId = exportTaskService.createTask(ExportTaskService.ExportTaskType.HOT_KEYWORDS, params);
        return ResponseEntity.ok(Map.of("taskId", taskId));
    }
    
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> createBatchTasks(
            @RequestBody List<Map<String, Object>> tasks) {
        List<ExportTaskService.ExportTaskType> taskTypes = new ArrayList<>();
        List<ExportTaskService.ExportTaskParams> paramsList = new ArrayList<>();
        
        for (Map<String, Object> task : tasks) {
            ExportTaskService.ExportTaskType taskType = ExportTaskService.ExportTaskType.valueOf(
                (String) task.get("taskType"));
            ExportTaskService.ExportTaskParams params = new ExportTaskService.ExportTaskParams();
            
            if (task.containsKey("userIds")) {
                @SuppressWarnings("unchecked")
                List<String> userIds = (List<String>) task.get("userIds");
                params.setUserIds(userIds);
            }
            if (task.containsKey("startTime")) {
                params.setStartTime(LocalDateTime.parse((String) task.get("startTime")));
            }
            if (task.containsKey("endTime")) {
                params.setEndTime(LocalDateTime.parse((String) task.get("endTime")));
            }
            if (task.containsKey("format")) {
                params.setFormat(DataExportService.ExportFormat.valueOf((String) task.get("format")));
            }
            if (task.containsKey("size")) {
                params.setSize(((Number) task.get("size")).intValue());
            }
            
            taskTypes.add(taskType);
            paramsList.add(params);
        }
        
        List<String> taskIds = exportTaskService.createBatchTasks(taskTypes, paramsList);
        String batchId = taskIds.get(0).split("-")[0]; // 使用第一个任务的UUID前缀作为批处理ID
        
        Map<String, Object> response = new HashMap<>();
        response.put("batchId", batchId);
        response.put("taskIds", taskIds);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/task/{taskId}")
    public ResponseEntity<ExportTaskService.ExportTaskResult> getTaskResult(@PathVariable String taskId) {
        return ResponseEntity.ok(exportTaskService.getTaskResult(taskId));
    }
    
    @GetMapping("/task/{taskId}/status")
    public ResponseEntity<Map<String, String>> getTaskStatus(@PathVariable String taskId) {
        ExportTaskService.ExportTaskStatus status = exportTaskService.getTaskStatus(taskId);
        return ResponseEntity.ok(Map.of("status", status.name()));
    }
    
    @GetMapping("/task/{taskId}/progress")
    public ResponseEntity<ExportProgress> getTaskProgress(@PathVariable String taskId) {
        return ResponseEntity.ok(exportTaskService.getTaskProgress(taskId));
    }
    
    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Map<String, Boolean>> cancelTask(@PathVariable String taskId) {
        boolean cancelled = exportTaskService.cancelTask(taskId);
        return ResponseEntity.ok(Map.of("cancelled", cancelled));
    }
    
    @GetMapping("/task/{taskId}/download")
    public ResponseEntity<byte[]> downloadTaskFile(@PathVariable String taskId) {
        ExportTaskService.ExportTaskResult result = exportTaskService.getTaskResult(taskId);
        if (result == null || result.getStatus() != ExportTaskService.ExportTaskStatus.COMPLETED) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            return createFileResponse(result.getFilePath(), getFileName(result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/batch/status")
    public ResponseEntity<Map<String, ExportTaskService.ExportTaskStatus>> getBatchTaskStatus(
            @RequestParam List<String> taskIds) {
        return ResponseEntity.ok(exportTaskService.getBatchTaskStatus(taskIds));
    }
    
    @GetMapping("/batch/results")
    public ResponseEntity<Map<String, ExportTaskService.ExportTaskResult>> getBatchTaskResults(
            @RequestParam List<String> taskIds) {
        return ResponseEntity.ok(exportTaskService.getBatchTaskResults(taskIds));
    }
    
    @GetMapping("/batch/progress")
    public ResponseEntity<Map<String, ExportProgress>> getBatchTaskProgress(
            @RequestParam List<String> taskIds) {
        return ResponseEntity.ok(exportTaskService.getBatchTaskProgress(taskIds));
    }
    
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Boolean>> cancelBatchTasks(
            @RequestParam List<String> taskIds) {
        return ResponseEntity.ok(exportTaskService.cancelBatchTasks(taskIds));
    }
    
    @GetMapping("/tasks")
    public ResponseEntity<List<String>> getAllTaskIds() {
        return ResponseEntity.ok(exportTaskService.getAllTaskIds());
    }
    
    @GetMapping("/tasks/expiring")
    public ResponseEntity<List<String>> getExpiringTaskIds(
            @RequestParam(defaultValue = "1") int daysThreshold) {
        return ResponseEntity.ok(exportTaskService.getExpiringTaskIds(daysThreshold));
    }
    
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Boolean>> cleanupTask(@PathVariable String taskId) {
        boolean cleaned = exportTaskService.cleanupTask(taskId);
        return ResponseEntity.ok(Map.of("cleaned", cleaned));
    }
    
    @DeleteMapping("/tasks")
    public ResponseEntity<Map<String, Boolean>> cleanupTasks(
            @RequestParam List<String> taskIds) {
        return ResponseEntity.ok(exportTaskService.cleanupTasks(taskIds));
    }
    
    @GetMapping("/tasks/expiry-days")
    public ResponseEntity<Map<String, Integer>> getTaskExpiryDays() {
        return ResponseEntity.ok(Map.of("days", exportTaskService.getTaskExpiryDays()));
    }
    
    @PutMapping("/tasks/expiry-days")
    public ResponseEntity<Map<String, Integer>> setTaskExpiryDays(
            @RequestParam int days) {
        exportTaskService.setTaskExpiryDays(days);
        return ResponseEntity.ok(Map.of("days", exportTaskService.getTaskExpiryDays()));
    }
    
    @GetMapping("/tasks/priority/{taskId}")
    public ResponseEntity<Map<String, String>> getTaskPriority(@PathVariable String taskId) {
        ExportTaskService.TaskPriority priority = exportTaskService.getTaskPriority(taskId);
        return ResponseEntity.ok(Map.of("priority", priority.name()));
    }
    
    @PutMapping("/tasks/priority/{taskId}")
    public ResponseEntity<Map<String, String>> setTaskPriority(
            @PathVariable String taskId,
            @RequestParam ExportTaskService.TaskPriority priority) {
        exportTaskService.setTaskPriority(taskId, priority);
        return ResponseEntity.ok(Map.of("priority", priority.name()));
    }
    
    @PutMapping("/tasks/priority")
    public ResponseEntity<Map<String, Boolean>> setBatchTaskPriority(
            @RequestParam List<String> taskIds,
            @RequestParam ExportTaskService.TaskPriority priority) {
        return ResponseEntity.ok(exportTaskService.setBatchTaskPriority(taskIds, priority));
    }
    
    @GetMapping("/tasks/by-priority/{priority}")
    public ResponseEntity<List<String>> getTasksByPriority(
            @PathVariable ExportTaskService.TaskPriority priority) {
        return ResponseEntity.ok(exportTaskService.getTasksByPriority(priority));
    }
    
    @GetMapping("/tasks/grouped-by-priority")
    public ResponseEntity<Map<ExportTaskService.TaskPriority, List<String>>> getTasksGroupedByPriority() {
        return ResponseEntity.ok(exportTaskService.getTasksGroupedByPriority());
    }
    
    @GetMapping("/queue/status")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        return ResponseEntity.ok(exportTaskService.getQueueStatus());
    }
    
    @GetMapping("/queue/concurrent-tasks")
    public ResponseEntity<Map<String, Integer>> getConcurrentTasks() {
        return ResponseEntity.ok(Map.of("maxTasks", exportTaskService.getMaxConcurrentTasks()));
    }
    
    @PutMapping("/queue/concurrent-tasks")
    public ResponseEntity<Map<String, Integer>> setMaxConcurrentTasks(
            @RequestParam int maxTasks) {
        exportTaskService.setMaxConcurrentTasks(maxTasks);
        return ResponseEntity.ok(Map.of("max", exportTaskService.getMaxConcurrentTasks()));
    }
    
    @GetMapping("/queue/pending-tasks")
    public ResponseEntity<Map<String, Integer>> getPendingTasksCount() {
        return ResponseEntity.ok(Map.of("count", exportTaskService.getPendingTasksCount()));
    }
    
    @PostMapping("/queue/pause")
    public ResponseEntity<Map<String, Boolean>> pauseQueue() {
        exportTaskService.pauseQueue();
        return ResponseEntity.ok(Map.of("paused", true));
    }
    
    @PostMapping("/queue/resume")
    public ResponseEntity<Map<String, Boolean>> resumeQueue() {
        exportTaskService.resumeQueue();
        return ResponseEntity.ok(Map.of("paused", false));
    }
    
    @GetMapping("/queue/paused")
    public ResponseEntity<Map<String, Boolean>> isQueuePaused() {
        return ResponseEntity.ok(Map.of("paused", exportTaskService.isQueuePaused()));
    }
    
    @GetMapping("/tasks/retry/count/{taskId}")
    public ResponseEntity<Map<String, Integer>> getTaskRetryCount(@PathVariable String taskId) {
        return ResponseEntity.ok(Map.of("count", exportTaskService.getTaskRetryCount(taskId)));
    }
    
    @GetMapping("/tasks/retry/max-count")
    public ResponseEntity<Map<String, Integer>> getMaxRetryCount() {
        return ResponseEntity.ok(Map.of("maxRetries", exportTaskService.getMaxRetryCount()));
    }
    
    @PutMapping("/tasks/retry/max-count")
    public ResponseEntity<Map<String, Integer>> setMaxRetryCount(@RequestParam int maxRetries) {
        exportTaskService.setMaxRetryCount(maxRetries);
        return ResponseEntity.ok(Map.of("maxRetries", exportTaskService.getMaxRetryCount()));
    }
    
    @GetMapping("/tasks/retry/interval")
    public ResponseEntity<Map<String, Integer>> getRetryInterval() {
        return ResponseEntity.ok(Map.of("interval", exportTaskService.getRetryInterval()));
    }
    
    @PutMapping("/tasks/retry/interval")
    public ResponseEntity<Map<String, Integer>> setRetryInterval(@RequestParam int seconds) {
        exportTaskService.setRetryInterval(seconds);
        return ResponseEntity.ok(Map.of("interval", exportTaskService.getRetryInterval()));
    }
    
    @PostMapping("/tasks/retry/{taskId}")
    public ResponseEntity<Map<String, Boolean>> retryTask(@PathVariable String taskId) {
        boolean retried = exportTaskService.retryTask(taskId);
        return ResponseEntity.ok(Map.of("retried", retried));
    }
    
    @PostMapping("/tasks/retry")
    public ResponseEntity<Map<String, Boolean>> retryTasks(@RequestParam List<String> taskIds) {
        return ResponseEntity.ok(exportTaskService.retryTasks(taskIds));
    }
    
    @GetMapping("/tasks/failed")
    public ResponseEntity<List<String>> getFailedTasks() {
        return ResponseEntity.ok(exportTaskService.getFailedTasks());
    }
    
    @GetMapping("/tasks/retry/history/{taskId}")
    public ResponseEntity<List<Map<String, Object>>> getTaskRetryHistory(@PathVariable String taskId) {
        return ResponseEntity.ok(exportTaskService.getTaskRetryHistory(taskId));
    }
    
    @GetMapping("/tasks/retry/status/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskRetryStatus(@PathVariable String taskId) {
        return ResponseEntity.ok(exportTaskService.getTaskRetryStatus(taskId));
    }
    
    @GetMapping("/tasks/resource/limits")
    public ResponseEntity<Map<String, Object>> getTaskResourceLimits() {
        return ResponseEntity.ok(exportTaskService.getTaskResourceLimits());
    }
    
    @PutMapping("/tasks/resource/limits")
    public ResponseEntity<Map<String, Object>> setTaskResourceLimits(
            @RequestBody Map<String, Object> limits) {
        exportTaskService.setTaskResourceLimits(limits);
        return ResponseEntity.ok(exportTaskService.getTaskResourceLimits());
    }
    
    @GetMapping("/tasks/resource/usage/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskResourceUsage(@PathVariable String taskId) {
        return ResponseEntity.ok(exportTaskService.getTaskResourceUsage(taskId));
    }
    
    @GetMapping("/tasks/resource/usage")
    public ResponseEntity<Map<String, Map<String, Object>>> getAllTasksResourceUsage() {
        return ResponseEntity.ok(exportTaskService.getAllTasksResourceUsage());
    }
    
    @GetMapping("/tasks/resource/system")
    public ResponseEntity<Map<String, Object>> getSystemResourceUsage() {
        return ResponseEntity.ok(exportTaskService.getSystemResourceUsage());
    }
    
    @GetMapping("/tasks/resource/exceeding/{taskId}")
    public ResponseEntity<Map<String, Boolean>> isTaskExceedingLimits(@PathVariable String taskId) {
        return ResponseEntity.ok(Map.of("exceeding", exportTaskService.isTaskExceedingLimits(taskId)));
    }
    
    @PostMapping("/tasks/resource/pause-exceeding")
    public ResponseEntity<Map<String, Integer>> pauseExceedingTasks() {
        exportTaskService.pauseExceedingTasks();
        return ResponseEntity.ok(Map.of("pausedCount", exportTaskService.getPausedTasks().size()));
    }
    
    @PostMapping("/tasks/resource/resume")
    public ResponseEntity<Map<String, Integer>> resumePausedTasks() {
        exportTaskService.resumePausedTasks();
        return ResponseEntity.ok(Map.of("resumedCount", exportTaskService.getPausedTasks().size()));
    }
    
    @GetMapping("/tasks/resource/paused")
    public ResponseEntity<List<String>> getPausedTasks() {
        return ResponseEntity.ok(exportTaskService.getPausedTasks());
    }
    
    @PutMapping("/tasks/resource/priority/{taskId}")
    public ResponseEntity<Map<String, String>> setTaskPriorityWithResourceCheck(
            @PathVariable String taskId,
            @RequestParam ExportTaskService.TaskPriority priority) {
        exportTaskService.setTaskPriorityWithResourceCheck(taskId, priority);
        return ResponseEntity.ok(Map.of("priority", priority.name()));
    }
    
    @GetMapping("/tasks/metrics/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskMetrics(@PathVariable String taskId) {
        return ResponseEntity.ok(exportTaskService.getTaskMetrics(taskId));
    }
    
    @GetMapping("/tasks/metrics")
    public ResponseEntity<Map<String, Map<String, Object>>> getAllTasksMetrics() {
        return ResponseEntity.ok(exportTaskService.getAllTasksMetrics());
    }
    
    @GetMapping("/tasks/metrics/system")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        return ResponseEntity.ok(exportTaskService.getSystemMetrics());
    }
    
    @GetMapping("/tasks/alert/rules")
    public ResponseEntity<List<Map<String, Object>>> getAlertRules() {
        return ResponseEntity.ok(exportTaskService.getAlertRules());
    }
    
    @PostMapping("/tasks/alert/rules")
    public ResponseEntity<Map<String, String>> addAlertRule(@RequestBody Map<String, Object> rule) {
        exportTaskService.addAlertRule(rule);
        return ResponseEntity.ok(Map.of("message", "告警规则添加成功"));
    }
    
    @PutMapping("/tasks/alert/rules/{ruleId}")
    public ResponseEntity<Map<String, String>> updateAlertRule(
            @PathVariable String ruleId,
            @RequestBody Map<String, Object> rule) {
        exportTaskService.updateAlertRule(ruleId, rule);
        return ResponseEntity.ok(Map.of("message", "告警规则更新成功"));
    }
    
    @DeleteMapping("/tasks/alert/rules/{ruleId}")
    public ResponseEntity<Map<String, String>> deleteAlertRule(@PathVariable String ruleId) {
        exportTaskService.deleteAlertRule(ruleId);
        return ResponseEntity.ok(Map.of("message", "告警规则删除成功"));
    }
    
    @GetMapping("/tasks/alert/history/{taskId}")
    public ResponseEntity<List<Map<String, Object>>> getAlertHistory(@PathVariable String taskId) {
        return ResponseEntity.ok(exportTaskService.getAlertHistory(taskId));
    }
    
    @GetMapping("/tasks/alert/history")
    public ResponseEntity<List<Map<String, Object>>> getAllAlertHistory() {
        return ResponseEntity.ok(exportTaskService.getAllAlertHistory());
    }
    
    @GetMapping("/tasks/alert/statistics")
    public ResponseEntity<Map<String, Object>> getAlertStatistics() {
        return ResponseEntity.ok(exportTaskService.getAlertStatistics());
    }
    
    @PutMapping("/tasks/alert/notification")
    public ResponseEntity<Map<String, String>> setAlertNotificationMethod(
            @RequestParam String method,
            @RequestBody Map<String, Object> config) {
        exportTaskService.setAlertNotificationMethod(method, config);
        return ResponseEntity.ok(Map.of("message", "告警通知方式设置成功"));
    }
    
    @GetMapping("/tasks/alert/notification")
    public ResponseEntity<Map<String, Object>> getAlertNotificationMethod() {
        return ResponseEntity.ok(exportTaskService.getAlertNotificationMethod());
    }
    
    @PostMapping("/tasks/alert/notification/test")
    public ResponseEntity<Map<String, Boolean>> testAlertNotification() {
        boolean success = exportTaskService.testAlertNotification();
        return ResponseEntity.ok(Map.of("success", success));
    }
    
    @PutMapping("/tasks/{taskId}/scheduling/strategy")
    public ResponseEntity<Map<String, Object>> setTaskSchedulingStrategy(
            @PathVariable String taskId,
            @RequestParam SchedulingStrategy strategy) {
        exportTaskService.setTaskSchedulingStrategy(taskId, strategy);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "strategy", strategy.name()
        ));
    }
    
    @GetMapping("/tasks/{taskId}/scheduling/strategy")
    public ResponseEntity<Map<String, Object>> getTaskSchedulingStrategy(
            @PathVariable String taskId) {
        SchedulingStrategy strategy = exportTaskService.getTaskSchedulingStrategy(taskId);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "strategy", strategy.name()
        ));
    }
    
    @PostMapping("/tasks/{taskId}/dependencies")
    public ResponseEntity<Map<String, Object>> addTaskDependency(
            @PathVariable String taskId,
            @RequestParam String dependencyTaskId) {
        exportTaskService.addTaskDependency(taskId, dependencyTaskId);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "dependencyTaskId", dependencyTaskId
        ));
    }
    
    @DeleteMapping("/tasks/{taskId}/dependencies")
    public ResponseEntity<Map<String, Object>> removeTaskDependency(
            @PathVariable String taskId,
            @RequestParam String dependencyTaskId) {
        exportTaskService.removeTaskDependency(taskId, dependencyTaskId);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "dependencyTaskId", dependencyTaskId
        ));
    }
    
    @GetMapping("/tasks/{taskId}/dependencies")
    public ResponseEntity<Map<String, Object>> getTaskDependencies(
            @PathVariable String taskId) {
        List<String> dependencies = exportTaskService.getTaskDependencies(taskId);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "dependencies", dependencies
        ));
    }
    
    @GetMapping("/tasks/{taskId}/dependent-tasks")
    public ResponseEntity<Map<String, Object>> getDependentTasks(
            @PathVariable String taskId) {
        List<String> dependentTasks = exportTaskService.getDependentTasks(taskId);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "dependentTasks", dependentTasks
        ));
    }
    
    @PostMapping("/tasks/{taskId}/schedule")
    public ResponseEntity<Map<String, Object>> scheduleTask(
            @PathVariable String taskId,
            @RequestParam String cronExpression) {
        exportTaskService.scheduleTask(taskId, cronExpression);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "cronExpression", cronExpression
        ));
    }
    
    @DeleteMapping("/tasks/{taskId}/schedule")
    public ResponseEntity<Map<String, Object>> cancelScheduledTask(
            @PathVariable String taskId) {
        exportTaskService.cancelScheduledTask(taskId);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "status", "CANCELLED"
        ));
    }
    
    @GetMapping("/tasks/{taskId}/schedule")
    public ResponseEntity<Map<String, Object>> getScheduledTaskInfo(
            @PathVariable String taskId) {
        Map<String, Object> info = exportTaskService.getScheduledTaskInfo(taskId);
        return ResponseEntity.ok(info);
    }
    
    @GetMapping("/tasks/schedule")
    public ResponseEntity<Map<String, Object>> getAllScheduledTasks() {
        List<Map<String, Object>> tasks = exportTaskService.getAllScheduledTasks();
        return ResponseEntity.ok(Map.of("tasks", tasks));
    }
    
    @PostMapping("/tasks/{taskId}/schedule/pause")
    public ResponseEntity<Map<String, Object>> pauseScheduledTask(
            @PathVariable String taskId) {
        exportTaskService.pauseScheduledTask(taskId);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "status", "PAUSED"
        ));
    }
    
    @PostMapping("/tasks/{taskId}/schedule/resume")
    public ResponseEntity<Map<String, Object>> resumeScheduledTask(
            @PathVariable String taskId) {
        exportTaskService.resumeScheduledTask(taskId);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "status", "RESUMED"
        ));
    }
    
    @PostMapping("/tasks/{taskId}/schedule/execute-now")
    public ResponseEntity<Map<String, Object>> executeScheduledTaskNow(
            @PathVariable String taskId) {
        exportTaskService.executeScheduledTaskNow(taskId);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "status", "EXECUTING"
        ));
    }
    
    @GetMapping("/tasks/{taskId}/scheduling/status")
    public ResponseEntity<Map<String, Object>> getTaskSchedulingStatus(
            @PathVariable String taskId) {
        Map<String, Object> status = exportTaskService.getTaskSchedulingStatus(taskId);
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/tasks/scheduling/status")
    public ResponseEntity<Map<String, Object>> getAllTasksSchedulingStatus() {
        Map<String, Map<String, Object>> status = exportTaskService.getAllTasksSchedulingStatus();
        return ResponseEntity.ok(Map.of("tasks", status));
    }
    
    @GetMapping("/formats")
    public ResponseEntity<List<ExportFormat>> getSupportedFormats() {
        return ResponseEntity.ok(exportTaskService.getSupportedExportFormats());
    }
    
    @PostMapping("/formats/custom")
    public ResponseEntity<Void> addCustomFormat(@RequestBody ExportFormat format) {
        exportTaskService.addCustomExportFormat(format);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/formats/custom/{formatName}")
    public ResponseEntity<Void> removeCustomFormat(@PathVariable String formatName) {
        exportTaskService.removeCustomExportFormat(formatName);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/formats/{formatName}/config")
    public ResponseEntity<Map<String, Object>> getFormatConfig(@PathVariable String formatName) {
        return ResponseEntity.ok(exportTaskService.getExportFormatConfig(formatName));
    }
    
    @PutMapping("/formats/{formatName}/config")
    public ResponseEntity<Void> setFormatConfig(
            @PathVariable String formatName,
            @RequestBody Map<String, Object> config) {
        exportTaskService.setExportFormatConfig(formatName, config);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/formats/{formatName}/template")
    public ResponseEntity<String> getFormatTemplate(@PathVariable String formatName) {
        return ResponseEntity.ok(exportTaskService.getExportFormatTemplate(formatName));
    }
    
    @PutMapping("/formats/{formatName}/template")
    public ResponseEntity<Void> setFormatTemplate(
            @PathVariable String formatName,
            @RequestBody String template) {
        exportTaskService.setExportFormatTemplate(formatName, template);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/formats/{formatName}/validate")
    public ResponseEntity<Boolean> validateFormatConfig(
            @PathVariable String formatName,
            @RequestBody Map<String, Object> config) {
        return ResponseEntity.ok(exportTaskService.validateExportFormatConfig(formatName, config));
    }
    
    @GetMapping("/formats/statistics")
    public ResponseEntity<Map<String, Object>> getFormatStatistics() {
        return ResponseEntity.ok(exportTaskService.getExportFormatStatistics());
    }
    
    /**
     * 设置线程池配置
     */
    @PutMapping("/thread-pool/config")
    public ResponseEntity<Void> setThreadPoolConfig(
            @RequestParam int corePoolSize,
            @RequestParam int maxPoolSize,
            @RequestParam int queueCapacity) {
        exportTaskService.setThreadPoolConfig(corePoolSize, maxPoolSize, queueCapacity);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取线程池配置和状态
     */
    @GetMapping("/thread-pool/config")
    public ResponseEntity<Map<String, Object>> getThreadPoolConfig() {
        return ResponseEntity.ok(exportTaskService.getThreadPoolConfig());
    }
    
    /**
     * 设置线程池拒绝策略
     */
    @PutMapping("/thread-pool/rejection-policy")
    public ResponseEntity<Void> setThreadPoolRejectionPolicy(@RequestParam String policy) {
        exportTaskService.setThreadPoolRejectionPolicy(policy);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 设置线程池空闲线程存活时间
     */
    @PutMapping("/thread-pool/keep-alive-time")
    public ResponseEntity<Void> setThreadPoolKeepAliveTime(
            @RequestParam long keepAliveTime,
            @RequestParam TimeUnit unit) {
        exportTaskService.setThreadPoolKeepAliveTime(keepAliveTime, unit);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 清理线程池
     */
    @PostMapping("/thread-pool/purge")
    public ResponseEntity<Void> purgeThreadPool() {
        exportTaskService.purgeThreadPool();
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取线程池状态
     */
    @GetMapping("/thread-pool/status")
    public ResponseEntity<Map<String, Boolean>> getThreadPoolStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("shutdown", exportTaskService.isThreadPoolShutdown());
        status.put("terminated", exportTaskService.isThreadPoolTerminated());
        return ResponseEntity.ok(status);
    }
    
    /**
     * 获取线程池指标
     */
    @GetMapping("/thread-pool/metrics")
    public ResponseEntity<Map<String, Object>> getThreadPoolMetrics() {
        return ResponseEntity.ok(exportTaskService.getThreadPoolMetrics());
    }
    
    /**
     * 获取线程池指标历史
     */
    @GetMapping("/thread-pool/metrics/history")
    public ResponseEntity<Map<String, List<Double>>> getThreadPoolMetricsHistory() {
        Map<String, Object> metrics = exportTaskService.getThreadPoolMetrics();
        @SuppressWarnings("unchecked")
        Map<String, List<Double>> history = (Map<String, List<Double>>) metrics.get("history");
        return ResponseEntity.ok(history);
    }
    
    /**
     * 获取线程池指标趋势
     */
    @GetMapping("/thread-pool/metrics/trend")
    public ResponseEntity<Map<String, Object>> getThreadPoolMetricsTrend(
            @RequestParam(defaultValue = "1") int hours) {
        Map<String, Object> metrics = exportTaskService.getThreadPoolMetrics();
        @SuppressWarnings("unchecked")
        Map<String, List<Double>> history = (Map<String, List<Double>>) metrics.get("history");
        
        Map<String, Object> trend = new HashMap<>();
        history.forEach((key, values) -> {
            if (!values.isEmpty()) {
                double first = values.get(0);
                double last = values.get(values.size() - 1);
                double change = last - first;
                double percentageChange = first != 0 ? (change / first) * 100 : 0;
                
                Map<String, Object> metricTrend = new HashMap<>();
                metricTrend.put("start", first);
                metricTrend.put("end", last);
                metricTrend.put("change", change);
                metricTrend.put("percentage_change", percentageChange);
                trend.put(key, metricTrend);
            }
        });
        
        return ResponseEntity.ok(trend);
    }
    
    /**
     * 关闭线程池
     */
    @PostMapping("/thread-pool/shutdown")
    public ResponseEntity<Void> shutdownThreadPool() {
        exportTaskService.shutdownThreadPool();
        return ResponseEntity.ok().build();
    }
    
    /**
     * 立即关闭线程池
     */
    @PostMapping("/thread-pool/shutdown-now")
    public ResponseEntity<Void> shutdownThreadPoolNow() {
        exportTaskService.shutdownThreadPoolNow();
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/batch/{batchId}/progress")
    public ResponseEntity<Map<String, Object>> getBatchProgress(@PathVariable String batchId) {
        return ResponseEntity.ok(exportTaskService.getBatchProgress(batchId));
    }
    
    @GetMapping("/batch/{batchId}/tasks")
    public ResponseEntity<List<String>> getBatchTasks(@PathVariable String batchId) {
        Map<String, Object> progress = exportTaskService.getBatchProgress(batchId);
        @SuppressWarnings("unchecked")
        Map<String, ExportProgress> progressMap = (Map<String, ExportProgress>) progress.get("progress");
        return ResponseEntity.ok(new ArrayList<>(progressMap.keySet()));
    }
    
    @GetMapping("/batch/{batchId}/status")
    public ResponseEntity<Map<String, Object>> getBatchStatus(@PathVariable String batchId) {
        Map<String, Object> progress = exportTaskService.getBatchProgress(batchId);
        Map<String, Object> status = new HashMap<>();
        
        int totalTasks = (int) progress.get("totalTasks");
        int completedTasks = (int) progress.get("completedTasks");
        int failedTasks = (int) progress.get("failedTasks");
        
        status.put("totalTasks", totalTasks);
        status.put("completedTasks", completedTasks);
        status.put("failedTasks", failedTasks);
        status.put("inProgressTasks", totalTasks - completedTasks - failedTasks);
        status.put("completionPercentage", totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0);
        status.put("startTime", progress.get("startTime"));
        status.put("duration", progress.get("duration"));
        status.put("errors", progress.get("errors"));
        
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/batch/{batchId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBatch(@PathVariable String batchId) {
        Map<String, Object> progress = exportTaskService.getBatchProgress(batchId);
        @SuppressWarnings("unchecked")
        Map<String, ExportProgress> progressMap = (Map<String, ExportProgress>) progress.get("progress");
        
        Map<String, Boolean> results = new HashMap<>();
        progressMap.keySet().forEach(taskId -> {
            results.put(taskId, exportTaskService.cancelTask(taskId));
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("batchId", batchId);
        response.put("cancelledTasks", results);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/batch/{batchId}/retry")
    public ResponseEntity<Map<String, Object>> retryBatch(@PathVariable String batchId) {
        Map<String, Object> progress = exportTaskService.getBatchProgress(batchId);
        @SuppressWarnings("unchecked")
        Map<String, ExportProgress> progressMap = (Map<String, ExportProgress>) progress.get("progress");
        
        Map<String, Boolean> results = new HashMap<>();
        progressMap.keySet().forEach(taskId -> {
            if (exportTaskService.getTaskStatus(taskId) == ExportTaskService.ExportTaskStatus.FAILED) {
                results.put(taskId, exportTaskService.retryTask(taskId));
            }
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("batchId", batchId);
        response.put("retriedTasks", results);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/task/{taskId}/chunks/progress")
    public ResponseEntity<Map<String, Object>> getChunkProgress(@PathVariable String taskId) {
        return ResponseEntity.ok(exportTaskService.getChunkProgress(taskId));
    }
    
    @GetMapping("/task/{taskId}/chunks/status")
    public ResponseEntity<Map<String, Object>> getChunkStatus(@PathVariable String taskId) {
        Map<String, Object> progress = exportTaskService.getChunkProgress(taskId);
        Map<String, Object> status = new HashMap<>();
        
        int totalChunks = (int) progress.get("totalChunks");
        int completedChunks = (int) progress.get("completedChunks");
        int failedChunks = (int) progress.get("failedChunks");
        
        status.put("totalChunks", totalChunks);
        status.put("completedChunks", completedChunks);
        status.put("failedChunks", failedChunks);
        status.put("inProgressChunks", totalChunks - completedChunks - failedChunks);
        status.put("completionPercentage", totalChunks > 0 ? (completedChunks * 100.0 / totalChunks) : 0);
        status.put("errors", progress.get("chunkErrors"));
        
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/task/{taskId}/chunks/retry")
    public ResponseEntity<Map<String, Object>> retryFailedChunks(@PathVariable String taskId) {
        Map<String, Object> progress = exportTaskService.getChunkProgress(taskId);
        @SuppressWarnings("unchecked")
        Map<Integer, String> chunkErrors = (Map<Integer, String>) progress.get("chunkErrors");
        
        Map<Integer, Boolean> results = new HashMap<>();
        chunkErrors.keySet().forEach(chunkIndex -> {
            try {
                // 重新处理失败的分片
                exportTaskService.processDataChunk(taskId, 
                    exportTaskService.getTaskResult(taskId).getTaskType(),
                    new ExportTaskService.ExportTaskParams(),
                    chunkIndex);
                results.put(chunkIndex, true);
            } catch (Exception e) {
                results.put(chunkIndex, false);
            }
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("retriedChunks", results);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/task/{taskId}/chunks/logs")
    public ResponseEntity<Map<String, Object>> getChunkLogs(@PathVariable String taskId) {
        return ResponseEntity.ok(exportTaskService.getChunkLogs(taskId));
    }
    
    @GetMapping("/task/{taskId}/chunks/logs/time-range")
    public ResponseEntity<Map<String, Object>> getChunkLogsByTimeRange(
            @PathVariable String taskId,
            @RequestParam long startTime,
            @RequestParam long endTime) {
        return ResponseEntity.ok(exportTaskService.getChunkLogsByTimeRange(taskId, startTime, endTime));
    }
    
    @GetMapping("/task/{taskId}/chunks/logs/event")
    public ResponseEntity<Map<String, Object>> getChunkLogsByEvent(
            @PathVariable String taskId,
            @RequestParam String event) {
        return ResponseEntity.ok(exportTaskService.getChunkLogsByEvent(taskId, event));
    }
    
    @PutMapping("/task/{taskId}/chunks/{chunkIndex}/priority")
    public ResponseEntity<Map<String, Object>> setChunkPriority(
            @PathVariable String taskId,
            @PathVariable int chunkIndex,
            @RequestParam int priority) {
        try {
            exportTaskService.setChunkPriority(taskId, chunkIndex, priority);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "分片优先级设置成功");
            response.put("taskId", taskId);
            response.put("chunkIndex", chunkIndex);
            response.put("priority", priority);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "设置分片优先级失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/task/{taskId}/chunks/{chunkIndex}/priority")
    public ResponseEntity<Map<String, Object>> getChunkPriority(
            @PathVariable String taskId,
            @PathVariable int chunkIndex) {
        try {
            int priority = exportTaskService.getChunkPriority(taskId, chunkIndex);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("chunkIndex", chunkIndex);
            response.put("priority", priority);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取分片优先级失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/task/{taskId}/chunks/priority/batch")
    public ResponseEntity<Map<String, Object>> setBatchChunkPriority(
            @PathVariable String taskId,
            @RequestBody Map<Integer, Integer> chunkPriorities) {
        try {
            Map<Integer, Boolean> results = exportTaskService.setBatchChunkPriority(taskId, chunkPriorities);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量设置分片优先级完成");
            response.put("taskId", taskId);
            response.put("results", results);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量设置分片优先级失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/task/{taskId}/chunks/priority")
    public ResponseEntity<Map<String, Object>> getAllChunkPriorities(
            @PathVariable String taskId) {
        try {
            Map<Integer, Integer> priorities = exportTaskService.getAllChunkPriorities(taskId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("priorities", priorities);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取所有分片优先级失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    private ResponseEntity<byte[]> createFileResponse(String filePath, String fileName) throws Exception {
        Path path = Paths.get(filePath);
        byte[] data = Files.readAllBytes(path);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaType(fileName));
        headers.setContentDispositionFormData("attachment", fileName);
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(data);
    }
    
    private String getFileName(ExportTaskService.ExportTaskResult result) {
        String prefix;
        switch (result.getTaskType()) {
            case SEARCH_STATISTICS:
                prefix = "search_statistics";
                break;
            case USER_BEHAVIOR:
                prefix = "user_behavior";
                break;
            case PERFORMANCE_METRICS:
                prefix = "performance_metrics";
                break;
            case HOT_KEYWORDS:
                prefix = "hot_keywords";
                break;
            default:
                prefix = "export";
        }
        
        String extension = result.getFilePath().substring(result.getFilePath().lastIndexOf("."));
        return prefix + extension;
    }
    
    private MediaType getMediaType(String fileName) {
        if (fileName.endsWith(".csv")) {
            return MediaType.parseMediaType("text/csv");
        } else if (fileName.endsWith(".xlsx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
} 