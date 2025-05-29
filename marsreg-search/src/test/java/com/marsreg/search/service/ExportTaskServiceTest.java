package com.marsreg.search.service;

import com.marsreg.search.service.impl.ExportTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportTaskServiceTest {

    @Mock
    private DataExportService dataExportService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    private ExportTaskService exportTaskService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        exportTaskService = new ExportTaskServiceImpl(dataExportService, redisTemplate);
    }

    @Test
    void createTask_ShouldCreateNewTask() {
        // 准备测试数据
        ExportTaskService.ExportTaskType taskType = ExportTaskService.ExportTaskType.SEARCH_STATISTICS;
        ExportTaskService.ExportTaskParams params = new ExportTaskService.ExportTaskParams();
        params.setStartTime(LocalDateTime.now());
        params.setEndTime(LocalDateTime.now().plusDays(1));
        params.setFormat(DataExportService.ExportFormat.CSV);

        // 执行测试
        String taskId = exportTaskService.createTask(taskType, params);

        // 验证结果
        assertNotNull(taskId);
        verify(valueOperations).set(anyString(), anyString());
        verify(hashOperations).putAll(anyString(), anyMap());
    }

    @Test
    void getTaskStatus_ShouldReturnCorrectStatus() {
        // 准备测试数据
        String taskId = "test-task-id";
        when(valueOperations.get(anyString())).thenReturn("PENDING");

        // 执行测试
        ExportTaskService.ExportTaskStatus status = exportTaskService.getTaskStatus(taskId);

        // 验证结果
        assertEquals(ExportTaskService.ExportTaskStatus.PENDING, status);
        verify(valueOperations).get(anyString());
    }

    @Test
    void setTaskPriority_ShouldUpdatePriority() {
        // 准备测试数据
        String taskId = "test-task-id";
        ExportTaskService.TaskPriority priority = ExportTaskService.TaskPriority.HIGH;

        // 执行测试
        exportTaskService.setTaskPriority(taskId, priority);

        // 验证结果
        verify(valueOperations).set(anyString(), eq(priority.name()));
    }

    @Test
    void getTaskMetrics_ShouldReturnMetrics() {
        // 准备测试数据
        String taskId = "test-task-id";
        Map<Object, Object> resultMap = new HashMap<>();
        resultMap.put("taskId", taskId);
        resultMap.put("status", "COMPLETED");
        resultMap.put("createTime", LocalDateTime.now().toString());

        when(hashOperations.entries(anyString())).thenReturn(resultMap);

        // 执行测试
        Map<String, Object> metrics = exportTaskService.getTaskMetrics(taskId);

        // 验证结果
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("status"));
        verify(hashOperations).entries(anyString());
    }

    @Test
    void addAlertRule_ShouldAddNewRule() {
        // 准备测试数据
        Map<String, Object> rule = new HashMap<>();
        rule.put("type", "EXECUTION_TIME");
        rule.put("threshold", 3600);
        rule.put("level", "WARNING");

        // 执行测试
        exportTaskService.addAlertRule(rule);

        // 验证结果
        verify(listOperations).rightPush(anyString(), any());
    }

    @Test
    void getAlertRules_ShouldReturnAllRules() {
        // 准备测试数据
        List<Object> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("type", "EXECUTION_TIME");
        rule.put("threshold", 3600);
        rules.add(rule);

        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(rules);

        // 执行测试
        List<Map<String, Object>> result = exportTaskService.getAlertRules();

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(listOperations).range(anyString(), anyLong(), anyLong());
    }

    @Test
    void testAlertNotification_ShouldReturnTrue() {
        // 准备测试数据
        Map<String, Object> notification = new HashMap<>();
        notification.put("method", "EMAIL");
        Map<String, Object> config = new HashMap<>();
        config.put("recipients", Arrays.asList("test@example.com"));
        notification.put("config", config);

        when(valueOperations.get(anyString())).thenReturn(notification);

        // 执行测试
        boolean result = exportTaskService.testAlertNotification();

        // 验证结果
        assertTrue(result);
        verify(valueOperations).get(anyString());
    }

    @Test
    void getSystemMetrics_ShouldReturnMetrics() {
        // 执行测试
        Map<String, Object> metrics = exportTaskService.getSystemMetrics();

        // 验证结果
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("resourceUsage"));
        assertTrue(metrics.containsKey("totalTasks"));
        assertTrue(metrics.containsKey("completedTasks"));
        assertTrue(metrics.containsKey("failedTasks"));
        assertTrue(metrics.containsKey("pendingTasks"));
        assertTrue(metrics.containsKey("alertStatistics"));
    }
} 