package com.marsreg.search.service;

import com.marsreg.search.model.ExportProgress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface ExportTaskService {
    /**
     * 创建导出任务
     *
     * @param taskType 任务类型
     * @param params 任务参数
     * @return 任务ID
     */
    String createTask(ExportTaskType taskType, ExportTaskParams params);
    
    /**
     * 批量创建导出任务
     */
    List<String> createBatchTasks(List<ExportTaskType> taskTypes, List<ExportTaskParams> paramsList);
    
    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    ExportTaskStatus getTaskStatus(String taskId);
    
    /**
     * 批量获取任务状态
     */
    Map<String, ExportTaskStatus> getBatchTaskStatus(List<String> taskIds);
    
    /**
     * 获取任务结果
     *
     * @param taskId 任务ID
     * @return 任务结果
     */
    ExportTaskResult getTaskResult(String taskId);
    
    /**
     * 批量获取任务结果
     */
    Map<String, ExportTaskResult> getBatchTaskResults(List<String> taskIds);
    
    /**
     * 获取任务进度
     *
     * @param taskId 任务ID
     * @return 任务进度
     */
    ExportProgress getTaskProgress(String taskId);
    
    /**
     * 批量获取任务进度
     */
    Map<String, ExportProgress> getBatchTaskProgress(List<String> taskIds);
    
    /**
     * 更新任务进度
     *
     * @param taskId 任务ID
     * @param completedSteps 已完成步骤数
     * @param currentStep 当前步骤
     */
    void updateTaskProgress(String taskId, int completedSteps, String currentStep);
    
    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTask(String taskId);
    
    /**
     * 批量取消任务
     */
    Map<String, Boolean> cancelBatchTasks(List<String> taskIds);
    
    /**
     * 清理过期任务
     */
    void cleanupExpiredTasks();
    
    /**
     * 获取任务过期时间（天）
     */
    int getTaskExpiryDays();
    
    /**
     * 设置任务过期时间（天）
     */
    void setTaskExpiryDays(int days);
    
    /**
     * 手动清理指定任务
     */
    boolean cleanupTask(String taskId);
    
    /**
     * 批量清理指定任务
     */
    Map<String, Boolean> cleanupTasks(List<String> taskIds);
    
    /**
     * 获取任务创建时间
     */
    LocalDateTime getTaskCreateTime(String taskId);
    
    /**
     * 获取所有任务ID
     */
    List<String> getAllTaskIds();
    
    /**
     * 获取即将过期的任务ID列表
     */
    List<String> getExpiringTaskIds(int daysThreshold);
    
    /**
     * 获取当前并发任务数
     */
    int getCurrentConcurrentTasks();
    
    /**
     * 获取最大并发任务数
     */
    int getMaxConcurrentTasks();
    
    /**
     * 设置最大并发任务数
     */
    void setMaxConcurrentTasks(int maxTasks);
    
    /**
     * 获取等待中的任务数
     */
    int getPendingTasksCount();
    
    /**
     * 获取任务队列状态
     */
    Map<String, Object> getQueueStatus();
    
    /**
     * 暂停任务队列
     */
    void pauseQueue();
    
    /**
     * 恢复任务队列
     */
    void resumeQueue();
    
    /**
     * 获取队列状态
     */
    boolean isQueuePaused();
    
    /**
     * 获取任务执行线程池状态
     */
    Map<String, Object> getThreadPoolStatus();
    
    /**
     * 获取任务重试次数
     */
    int getTaskRetryCount(String taskId);
    
    /**
     * 获取最大重试次数
     */
    int getMaxRetryCount();
    
    /**
     * 设置最大重试次数
     */
    void setMaxRetryCount(int maxRetries);
    
    /**
     * 获取重试间隔（秒）
     */
    int getRetryInterval();
    
    /**
     * 设置重试间隔（秒）
     */
    void setRetryInterval(int seconds);
    
    /**
     * 手动重试任务
     */
    boolean retryTask(String taskId);
    
    /**
     * 批量重试任务
     */
    Map<String, Boolean> retryTasks(List<String> taskIds);
    
    /**
     * 获取失败任务列表
     */
    List<String> getFailedTasks();
    
    /**
     * 获取任务重试历史
     */
    List<Map<String, Object>> getTaskRetryHistory(String taskId);
    
    /**
     * 获取任务重试状态
     */
    Map<String, Object> getTaskRetryStatus(String taskId);
    
    /**
     * 获取任务资源限制
     */
    Map<String, Object> getTaskResourceLimits();
    
    /**
     * 设置任务资源限制
     */
    void setTaskResourceLimits(Map<String, Object> limits);
    
    /**
     * 获取任务资源使用情况
     */
    Map<String, Object> getTaskResourceUsage(String taskId);
    
    /**
     * 获取所有任务的资源使用情况
     */
    Map<String, Map<String, Object>> getAllTasksResourceUsage();
    
    /**
     * 获取系统资源使用情况
     */
    Map<String, Object> getSystemResourceUsage();
    
    /**
     * 检查任务是否超过资源限制
     */
    boolean isTaskExceedingLimits(String taskId);
    
    /**
     * 暂停超过资源限制的任务
     */
    void pauseExceedingTasks();
    
    /**
     * 恢复暂停的任务
     */
    void resumePausedTasks();
    
    /**
     * 获取被暂停的任务列表
     */
    List<String> getPausedTasks();
    
    /**
     * 设置任务优先级（考虑资源限制）
     */
    void setTaskPriorityWithResourceCheck(String taskId, TaskPriority priority);
    
    /**
     * 获取任务监控指标
     */
    Map<String, Object> getTaskMetrics(String taskId);
    
    /**
     * 获取所有任务的监控指标
     */
    Map<String, Map<String, Object>> getAllTasksMetrics();
    
    /**
     * 获取系统监控指标
     */
    Map<String, Object> getSystemMetrics();
    
    /**
     * 获取告警规则
     */
    List<Map<String, Object>> getAlertRules();
    
    /**
     * 添加告警规则
     */
    void addAlertRule(Map<String, Object> rule);
    
    /**
     * 更新告警规则
     */
    void updateAlertRule(String ruleId, Map<String, Object> rule);
    
    /**
     * 删除告警规则
     */
    void deleteAlertRule(String ruleId);
    
    /**
     * 获取告警历史
     */
    List<Map<String, Object>> getAlertHistory(String taskId);
    
    /**
     * 获取所有告警历史
     */
    List<Map<String, Object>> getAllAlertHistory();
    
    /**
     * 获取告警统计
     */
    Map<String, Object> getAlertStatistics();
    
    /**
     * 设置告警通知方式
     */
    void setAlertNotificationMethod(String method, Map<String, Object> config);
    
    /**
     * 获取告警通知方式
     */
    Map<String, Object> getAlertNotificationMethod();
    
    /**
     * 测试告警通知
     */
    boolean testAlertNotification();
    
    /**
     * 设置任务调度策略
     */
    void setTaskSchedulingStrategy(String taskId, SchedulingStrategy strategy);
    
    /**
     * 获取任务调度策略
     */
    SchedulingStrategy getTaskSchedulingStrategy(String taskId);
    
    /**
     * 添加任务依赖关系
     */
    void addTaskDependency(String taskId, String dependencyTaskId);
    
    /**
     * 移除任务依赖关系
     */
    void removeTaskDependency(String taskId, String dependencyTaskId);
    
    /**
     * 获取任务依赖列表
     */
    List<String> getTaskDependencies(String taskId);
    
    /**
     * 获取依赖此任务的任务列表
     */
    List<String> getDependentTasks(String taskId);
    
    /**
     * 设置定时任务
     */
    void scheduleTask(String taskId, String cronExpression);
    
    /**
     * 取消定时任务
     */
    void cancelScheduledTask(String taskId);
    
    /**
     * 获取定时任务信息
     */
    Map<String, Object> getScheduledTaskInfo(String taskId);
    
    /**
     * 获取所有定时任务
     */
    List<Map<String, Object>> getAllScheduledTasks();
    
    /**
     * 暂停定时任务
     */
    void pauseScheduledTask(String taskId);
    
    /**
     * 恢复定时任务
     */
    void resumeScheduledTask(String taskId);
    
    /**
     * 立即执行定时任务
     */
    void executeScheduledTaskNow(String taskId);
    
    /**
     * 获取任务调度状态
     */
    Map<String, Object> getTaskSchedulingStatus(String taskId);
    
    /**
     * 获取所有任务的调度状态
     */
    Map<String, Map<String, Object>> getAllTasksSchedulingStatus();
    
    /**
     * 获取支持的导出格式列表
     */
    List<ExportFormat> getSupportedExportFormats();
    
    /**
     * 添加自定义导出格式
     */
    void addCustomExportFormat(ExportFormat format);
    
    /**
     * 移除自定义导出格式
     */
    void removeCustomExportFormat(String formatName);
    
    /**
     * 获取导出格式配置
     */
    Map<String, Object> getExportFormatConfig(String formatName);
    
    /**
     * 设置导出格式配置
     */
    void setExportFormatConfig(String formatName, Map<String, Object> config);
    
    /**
     * 获取导出格式模板
     */
    String getExportFormatTemplate(String formatName);
    
    /**
     * 设置导出格式模板
     */
    void setExportFormatTemplate(String formatName, String template);
    
    /**
     * 验证导出格式配置
     */
    boolean validateExportFormatConfig(String formatName, Map<String, Object> config);
    
    /**
     * 获取导出格式转换器
     */
    ExportFormatConverter getExportFormatConverter(String formatName);
    
    /**
     * 注册导出格式转换器
     */
    void registerExportFormatConverter(String formatName, ExportFormatConverter converter);
    
    /**
     * 获取导出格式统计信息
     */
    Map<String, Object> getExportFormatStatistics();
    
    /**
     * 设置线程池配置
     * @param corePoolSize 核心线程数
     * @param maxPoolSize 最大线程数
     * @param queueCapacity 队列容量
     */
    void setThreadPoolConfig(int corePoolSize, int maxPoolSize, int queueCapacity);
    
    /**
     * 获取线程池配置和状态
     * @return 线程池配置和状态信息
     */
    Map<String, Object> getThreadPoolConfig();
    
    /**
     * 设置线程池拒绝策略
     * @param policy 拒绝策略（ABORT/CALLER_RUNS/DISCARD/DISCARD_OLDEST）
     */
    void setThreadPoolRejectionPolicy(String policy);
    
    /**
     * 设置线程池空闲线程存活时间
     * @param keepAliveTime 存活时间
     * @param unit 时间单位
     */
    void setThreadPoolKeepAliveTime(long keepAliveTime, TimeUnit unit);
    
    /**
     * 清理线程池中的取消任务
     */
    void purgeThreadPool();
    
    /**
     * 检查线程池是否已关闭
     * @return 是否已关闭
     */
    boolean isThreadPoolShutdown();
    
    /**
     * 检查线程池是否已终止
     * @return 是否已终止
     */
    boolean isThreadPoolTerminated();
    
    /**
     * 关闭线程池
     */
    void shutdownThreadPool();
    
    /**
     * 立即关闭线程池
     */
    void shutdownThreadPoolNow();
    
    /**
     * 设置分片优先级
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @param priority 优先级值（-10到10之间）
     */
    void setChunkPriority(String taskId, int chunkIndex, int priority);
    
    /**
     * 获取分片优先级
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @return 优先级值
     */
    int getChunkPriority(String taskId, int chunkIndex);
    
    /**
     * 批量设置分片优先级
     * @param taskId 任务ID
     * @param chunkPriorities 分片优先级映射（分片索引 -> 优先级值）
     * @return 设置结果映射（分片索引 -> 是否成功）
     */
    Map<Integer, Boolean> setBatchChunkPriority(String taskId, Map<Integer, Integer> chunkPriorities);
    
    /**
     * 获取任务的所有分片优先级
     * @param taskId 任务ID
     * @return 分片优先级映射（分片索引 -> 优先级值）
     */
    Map<Integer, Integer> getAllChunkPriorities(String taskId);
    
    /**
     * 获取分片处理的性能指标
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @return 性能指标数据
     */
    Map<String, Object> getChunkPerformanceMetrics(String taskId, int chunkIndex);

    /**
     * 获取分片处理的性能分析报告
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @return 性能分析报告
     */
    Map<String, Object> getChunkPerformanceReport(String taskId, int chunkIndex);

    /**
     * 获取分片处理的性能优化建议
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @return 性能优化建议
     */
    List<Map<String, Object>> getChunkOptimizationSuggestions(String taskId, int chunkIndex);

    /**
     * 应用性能优化建议
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @param suggestionId 建议ID
     * @return 优化结果
     */
    Map<String, Object> applyOptimizationSuggestion(String taskId, int chunkIndex, String suggestionId);

    /**
     * 获取分片处理的性能趋势数据
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @param timeRange 时间范围（小时）
     * @return 性能趋势数据
     */
    Map<String, List<Map<String, Object>>> getChunkPerformanceTrend(String taskId, int chunkIndex, int timeRange);

    /**
     * 设置分片处理的性能告警阈值
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @param thresholds 告警阈值配置
     */
    void setChunkPerformanceThresholds(String taskId, int chunkIndex, Map<String, Object> thresholds);

    /**
     * 获取分片处理的性能告警阈值
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @return 告警阈值配置
     */
    Map<String, Object> getChunkPerformanceThresholds(String taskId, int chunkIndex);

    /**
     * 获取分片处理的性能告警历史
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @return 告警历史记录
     */
    List<Map<String, Object>> getChunkPerformanceAlerts(String taskId, int chunkIndex);

    /**
     * 获取分片处理的性能基准数据
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @return 性能基准数据
     */
    Map<String, Object> getChunkPerformanceBaseline(String taskId, int chunkIndex);

    /**
     * 更新分片处理的性能基准数据
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @param baseline 基准数据
     */
    void updateChunkPerformanceBaseline(String taskId, int chunkIndex, Map<String, Object> baseline);

    /**
     * 获取分片处理的性能对比报告
     * @param taskId 任务ID
     * @param chunkIndex 分片索引
     * @param compareTaskId 对比任务ID
     * @param compareChunkIndex 对比分片索引
     * @return 性能对比报告
     */
    Map<String, Object> getChunkPerformanceComparison(String taskId, int chunkIndex, 
        String compareTaskId, int compareChunkIndex);
    
    /**
     * 获取线程池指标
     * @return 线程池指标信息
     */
    Map<String, Object> getThreadPoolMetrics();
    
    /**
     * 获取批量任务进度
     * @param batchId 批量任务ID
     * @return 批量任务进度信息
     */
    Map<String, Object> getBatchProgress(String batchId);

    /**
     * 获取任务分片进度
     * @param taskId 任务ID
     * @return 分片进度信息
     */
    Map<String, Object> getChunkProgress(String taskId);

    /**
     * 获取指定时间范围内的分片日志
     * @param taskId 任务ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分片日志信息
     */
    Map<String, Object> getChunkLogsByTimeRange(String taskId, long startTime, long endTime);

    /**
     * 获取任务分片日志
     * @param taskId 任务ID
     * @return 分片日志信息
     */
    Map<String, Object> getChunkLogs(String taskId);

    /**
     * 获取指定事件类型的分片日志
     * @param taskId 任务ID
     * @param event 事件类型
     * @return 分片日志信息
     */
    Map<String, Object> getChunkLogsByEvent(String taskId, String event);

    /**
     * 处理数据分片
     * @param taskId 任务ID
     * @param taskType 任务类型
     * @param params 任务参数
     * @param chunkIndex 分片索引
     * @return 处理结果
     */
    Map<String, Object> processDataChunk(String taskId, ExportTaskType taskType, ExportTaskParams params, Integer chunkIndex);
    
    enum ExportTaskType {
        SEARCH_STATISTICS,
        USER_BEHAVIOR,
        PERFORMANCE_METRICS,
        HOT_KEYWORDS
    }
    
    enum ExportTaskStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    enum TaskPriority {
        LOW(0),
        NORMAL(1),
        HIGH(2),
        URGENT(3);
        
        private final int value;
        
        TaskPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    enum SchedulingStrategy {
        /**
         * 先进先出
         */
        FIFO,
        
        /**
         * 优先级优先
         */
        PRIORITY,
        
        /**
         * 资源利用率优先
         */
        RESOURCE_EFFICIENT,
        
        /**
         * 依赖关系优先
         */
        DEPENDENCY_BASED,
        
        /**
         * 混合策略
         */
        HYBRID
    }
    
    enum ExportFormat {
        CSV("csv", "text/csv", "CSV文件"),
        EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel文件"),
        JSON("json", "application/json", "JSON文件"),
        XML("xml", "application/xml", "XML文件"),
        PDF("pdf", "application/pdf", "PDF文件"),
        CUSTOM("custom", "application/octet-stream", "自定义格式");
        
        private final String extension;
        private final String mimeType;
        private final String description;
        
        ExportFormat(String extension, String mimeType, String description) {
            this.extension = extension;
            this.mimeType = mimeType;
            this.description = description;
        }
        
        public String getExtension() {
            return extension;
        }
        
        public String getMimeType() {
            return mimeType;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 导出格式转换器接口
     */
    interface ExportFormatConverter {
        /**
         * 转换数据为指定格式
         */
        byte[] convert(Object data, Map<String, Object> config) throws Exception;
        
        /**
         * 获取转换器支持的配置选项
         */
        Map<String, Object> getSupportedConfig();
        
        /**
         * 验证配置是否有效
         */
        boolean validateConfig(Map<String, Object> config);
    }
    
    class ExportTaskParams {
        private List<String> userIds;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private DataExportService.ExportFormat format;
        private int size;
        private TaskPriority priority = TaskPriority.NORMAL;
        
        public List<String> getUserIds() {
            return userIds;
        }
        
        public void setUserIds(List<String> userIds) {
            this.userIds = userIds;
        }
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }
        
        public LocalDateTime getEndTime() {
            return endTime;
        }
        
        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }
        
        public DataExportService.ExportFormat getFormat() {
            return format;
        }
        
        public void setFormat(DataExportService.ExportFormat format) {
            this.format = format;
        }
        
        public int getSize() {
            return size;
        }
        
        public void setSize(int size) {
            this.size = size;
        }
        
        public TaskPriority getPriority() {
            return priority;
        }
        
        public void setPriority(TaskPriority priority) {
            this.priority = priority;
        }
    }
    
    class ExportTaskResult {
        private String taskId;
        private ExportTaskType taskType;
        private ExportTaskStatus status;
        private String filePath;
        private String errorMessage;
        private LocalDateTime createTime;
        private LocalDateTime completeTime;
        
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
        public ExportTaskType getTaskType() {
            return taskType;
        }
        
        public void setTaskType(ExportTaskType taskType) {
            this.taskType = taskType;
        }
        
        public ExportTaskStatus getStatus() {
            return status;
        }
        
        public void setStatus(ExportTaskStatus status) {
            this.status = status;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public LocalDateTime getCreateTime() {
            return createTime;
        }
        
        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
        
        public LocalDateTime getCompleteTime() {
            return completeTime;
        }
        
        public void setCompleteTime(LocalDateTime completeTime) {
            this.completeTime = completeTime;
        }
    }
    
    /**
     * 获取任务优先级
     */
    TaskPriority getTaskPriority(String taskId);
    
    /**
     * 设置任务优先级
     */
    void setTaskPriority(String taskId, TaskPriority priority);
    
    /**
     * 批量设置任务优先级
     */
    Map<String, Boolean> setBatchTaskPriority(List<String> taskIds, TaskPriority priority);
    
    /**
     * 获取指定优先级的任务列表
     */
    List<String> getTasksByPriority(TaskPriority priority);
    
    /**
     * 获取所有任务按优先级分组
     */
    Map<TaskPriority, List<String>> getTasksGroupedByPriority();
} 