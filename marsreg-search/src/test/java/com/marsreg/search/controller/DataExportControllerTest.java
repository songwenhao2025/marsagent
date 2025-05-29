package com.marsreg.search.controller;

import com.marsreg.search.model.ExportProgress;
import com.marsreg.search.service.DataExportService;
import com.marsreg.search.service.ExportTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataExportControllerTest {

    @Mock
    private DataExportService dataExportService;

    @Mock
    private ExportTaskService exportTaskService;

    @InjectMocks
    private DataExportController dataExportController;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
    }

    @Test
    void exportSearchStatistics_ShouldCreateTask() {
        // 准备测试数据
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(1);
        String taskId = "test-task-id";

        when(exportTaskService.createTask(any(), any())).thenReturn(taskId);

        // 执行测试
        ResponseEntity<Map<String, String>> response = dataExportController.exportSearchStatistics(
            startTime, endTime, DataExportService.ExportFormat.CSV);

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(taskId, response.getBody().get("taskId"));
        verify(exportTaskService).createTask(any(), any());
    }

    @Test
    void getTaskStatus_ShouldReturnStatus() {
        // 准备测试数据
        String taskId = "test-task-id";
        when(exportTaskService.getTaskStatus(taskId))
            .thenReturn(ExportTaskService.ExportTaskStatus.PENDING);

        // 执行测试
        ResponseEntity<Map<String, String>> response = dataExportController.getTaskStatus(taskId);

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("PENDING", response.getBody().get("status"));
        verify(exportTaskService).getTaskStatus(taskId);
    }

    @Test
    void getTaskProgress_ShouldReturnProgress() {
        // 准备测试数据
        String taskId = "test-task-id";
        ExportProgress progress = new ExportProgress();
        progress.setTaskId(taskId);
        progress.setTotalSteps(3);
        progress.setCompletedSteps(1);

        when(exportTaskService.getTaskProgress(taskId)).thenReturn(progress);

        // 执行测试
        ResponseEntity<ExportProgress> response = dataExportController.getTaskProgress(taskId);

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(taskId, response.getBody().getTaskId());
        assertEquals(3, response.getBody().getTotalSteps());
        assertEquals(1, response.getBody().getCompletedSteps());
        verify(exportTaskService).getTaskProgress(taskId);
    }

    @Test
    void setTaskPriority_ShouldUpdatePriority() {
        // 准备测试数据
        String taskId = "test-task-id";
        ExportTaskService.TaskPriority priority = ExportTaskService.TaskPriority.HIGH;

        // 执行测试
        ResponseEntity<Map<String, String>> response = dataExportController.setTaskPriority(taskId, priority);

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(priority.name(), response.getBody().get("priority"));
        verify(exportTaskService).setTaskPriority(taskId, priority);
    }

    @Test
    void getQueueStatus_ShouldReturnStatus() {
        // 准备测试数据
        Map<String, Object> status = new HashMap<>();
        status.put("currentConcurrentTasks", 2);
        status.put("maxConcurrentTasks", 5);
        status.put("pendingTasks", 3);

        when(exportTaskService.getQueueStatus()).thenReturn(status);

        // 执行测试
        ResponseEntity<Map<String, Object>> response = dataExportController.getQueueStatus();

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().get("currentConcurrentTasks"));
        assertEquals(5, response.getBody().get("maxConcurrentTasks"));
        assertEquals(3, response.getBody().get("pendingTasks"));
        verify(exportTaskService).getQueueStatus();
    }

    @Test
    void getAlertRules_ShouldReturnRules() {
        // 准备测试数据
        List<Map<String, Object>> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("type", "EXECUTION_TIME");
        rule.put("threshold", 3600);
        rules.add(rule);

        when(exportTaskService.getAlertRules()).thenReturn(rules);

        // 执行测试
        ResponseEntity<List<Map<String, Object>>> response = dataExportController.getAlertRules();

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("EXECUTION_TIME", response.getBody().get(0).get("type"));
        assertEquals(3600, response.getBody().get(0).get("threshold"));
        verify(exportTaskService).getAlertRules();
    }

    @Test
    void addAlertRule_ShouldAddRule() {
        // 准备测试数据
        Map<String, Object> rule = new HashMap<>();
        rule.put("type", "EXECUTION_TIME");
        rule.put("threshold", 3600);
        rule.put("level", "WARNING");

        // 执行测试
        ResponseEntity<Map<String, String>> response = dataExportController.addAlertRule(rule);

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("告警规则添加成功", response.getBody().get("message"));
        verify(exportTaskService).addAlertRule(rule);
    }

    @Test
    void getSystemMetrics_ShouldReturnMetrics() {
        // 准备测试数据
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("resourceUsage", new HashMap<>());
        metrics.put("totalTasks", 10);
        metrics.put("completedTasks", 5);
        metrics.put("failedTasks", 1);
        metrics.put("pendingTasks", 4);

        when(exportTaskService.getSystemMetrics()).thenReturn(metrics);

        // 执行测试
        ResponseEntity<Map<String, Object>> response = dataExportController.getSystemMetrics();

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(10, response.getBody().get("totalTasks"));
        assertEquals(5, response.getBody().get("completedTasks"));
        assertEquals(1, response.getBody().get("failedTasks"));
        assertEquals(4, response.getBody().get("pendingTasks"));
        verify(exportTaskService).getSystemMetrics();
    }
} 