package com.marsreg.search.service.impl;

import com.marsreg.search.model.ExportProgress;
import com.marsreg.search.service.DataExportService;
import com.marsreg.search.service.ExportTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ScheduledFuture;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.RejectedExecutionHandler;
import java.lang.reflect.Field;
import com.alibaba.fastjson.JSON;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTaskServiceImpl implements ExportTaskService {

    private final DataExportService dataExportService;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String TASK_KEY_PREFIX = "export:task:";
    private static final String TASK_STATUS_KEY = "export:task:status:";
    private static final String TASK_RESULT_KEY = "export:task:result:";
    private static final String TASK_PROGRESS_KEY = "export:task:progress:";
    private static final String TASK_PRIORITY_KEY = "export:task:priority:";
    private static final String TASK_RETRY_COUNT_KEY = "export:task:retry:count:";
    private static final String TASK_RETRY_HISTORY_KEY = "export:task:retry:history:";
    private static final int TASK_EXPIRY_DAYS = 7;
    private static final int MAX_CONCURRENT_TASKS = 5;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_RETRY_INTERVAL = 60; // 60秒
    
    private static final String TASK_RESOURCE_LIMITS_KEY = "export:task:resource:limits";
    private static final String TASK_RESOURCE_USAGE_KEY = "export:task:resource:usage:";
    private static final String TASK_PAUSED_KEY = "export:task:paused:";
    
    private static final double DEFAULT_CPU_LIMIT = 80.0; // CPU使用率限制（百分比）
    private static final double DEFAULT_MEMORY_LIMIT = 80.0; // 内存使用率限制（百分比）
    private static final int DEFAULT_THREAD_LIMIT = 10; // 每个任务最大线程数
    private static final long DEFAULT_DISK_SPACE_LIMIT = 1024 * 1024 * 1024; // 磁盘空间限制（1GB）
    
    private static final String TASK_METRICS_KEY = "export:task:metrics:";
    private static final String ALERT_RULES_KEY = "export:alert:rules";
    private static final String ALERT_HISTORY_KEY = "export:alert:history:";
    private static final String ALERT_NOTIFICATION_KEY = "export:alert:notification";
    
    private static final String TASK_SCHEDULING_STRATEGY_KEY = "export:task:scheduling:strategy:";
    private static final String TASK_DEPENDENCIES_KEY = "export:task:dependencies:";
    private static final String TASK_SCHEDULE_KEY = "export:task:schedule:";
    
    private static final String EXPORT_FORMAT_CONFIG_KEY = "export:format:config:";
    private static final String EXPORT_FORMAT_TEMPLATE_KEY = "export:format:template:";
    private static final String EXPORT_FORMAT_STATS_KEY = "export:format:stats:";
    
    private static final int DEFAULT_CORE_POOL_SIZE = 5;
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_QUEUE_CAPACITY = 100;
    private static final long DEFAULT_KEEP_ALIVE_TIME = 60L;
    
    // 动态调整相关配置
    private static final int MIN_CORE_POOL_SIZE = 2;
    private static final int MAX_CORE_POOL_SIZE = 20;
    private static final int MIN_QUEUE_CAPACITY = 50;
    private static final int MAX_QUEUE_CAPACITY = 500;
    private static final double CPU_THRESHOLD_HIGH = 80.0;
    private static final double CPU_THRESHOLD_LOW = 20.0;
    private static final double QUEUE_UTILIZATION_THRESHOLD_HIGH = 0.8;
    private static final double QUEUE_UTILIZATION_THRESHOLD_LOW = 0.2;
    private static final int ADJUSTMENT_STEP = 2;
    private static final long ADJUSTMENT_INTERVAL = 60; // 60秒
    
    // 监控相关配置
    private static final long METRICS_COLLECTION_INTERVAL = 10; // 10秒
    private static final int METRICS_HISTORY_SIZE = 100; // 保留最近100个指标点
    
    private ThreadPoolExecutor taskExecutor;
    private final Object executorLock = new Object();
    private ScheduledFuture<?> dynamicAdjustmentTask;
    private ScheduledFuture<?> metricsCollectionTask;
    
    // 监控指标存储
    private final ConcurrentHashMap<String, CircularBuffer<Double>> metricsHistory = new ConcurrentHashMap<>();
    private final CircularBuffer<ThreadPoolMetrics> threadPoolMetricsHistory = new CircularBuffer<>(METRICS_HISTORY_SIZE);
    
    private final ExecutorService taskExecutorService = Executors.newFixedThreadPool(MAX_CONCURRENT_TASKS);
    private final ConcurrentHashMap<String, CompletableFuture<Void>> runningTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ExportProgress> taskProgress = new ConcurrentHashMap<>();

    private int taskExpiryDays = TASK_EXPIRY_DAYS;
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private int retryInterval = DEFAULT_RETRY_INTERVAL;
    
    private volatile boolean queuePaused = false;
    private final Object queueLock = new Object();
    
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final AtomicInteger totalThreadCount = new AtomicInteger(0);

    private final ConcurrentHashMap<String, AtomicLong> taskExecutionTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> taskErrorCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> taskRetryCount = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private final Map<String, ExportFormatConverter> formatConverters = new ConcurrentHashMap<>();
    private final Set<ExportFormat> customFormats = ConcurrentHashMap.newKeySet();

    private static final int BATCH_SIZE = 100; // 每批处理的任务数
    private static final int MAX_PARALLEL_BATCHES = 5; // 最大并行批次数
    private static final long BATCH_TIMEOUT = 3600; // 批处理超时时间（秒）
    
    private final ConcurrentHashMap<String, BatchExportContext> batchContexts = new ConcurrentHashMap<>();
    
    private static class BatchExportContext {
        private final List<String> taskIds;
        private final AtomicInteger completedCount;
        private final AtomicInteger failedCount;
        private final LocalDateTime startTime;
        private final Map<String, ExportProgress> progressMap;
        private final Map<String, String> errorMap;
        
        public BatchExportContext(List<String> taskIds) {
            this.taskIds = taskIds;
            this.completedCount = new AtomicInteger(0);
            this.failedCount = new AtomicInteger(0);
            this.startTime = LocalDateTime.now();
            this.progressMap = new ConcurrentHashMap<>();
            this.errorMap = new ConcurrentHashMap<>();
        }
    }
    
    @PostConstruct
    public void init() {
        taskExecutor = new ThreadPoolExecutor(
            DEFAULT_CORE_POOL_SIZE,
            DEFAULT_MAX_POOL_SIZE,
            DEFAULT_KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "export-task-thread-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 初始化监控指标存储
        initializeMetricsStorage();
        
        // 启动动态调整任务
        startDynamicAdjustment();
        
        // 启动指标收集任务
        startMetricsCollection();
    }
    
    @PreDestroy
    public void destroy() {
        // 停止动态调整任务
        stopDynamicAdjustment();
        
        // 停止指标收集任务
        stopMetricsCollection();
        
        if (taskExecutor != null) {
            taskExecutor.shutdown();
            try {
                if (!taskExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    taskExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                taskExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void initializeMetricsStorage() {
        metricsHistory.put("cpu_usage", new CircularBuffer<>(METRICS_HISTORY_SIZE));
        metricsHistory.put("memory_usage", new CircularBuffer<>(METRICS_HISTORY_SIZE));
        metricsHistory.put("queue_utilization", new CircularBuffer<>(METRICS_HISTORY_SIZE));
        metricsHistory.put("active_threads", new CircularBuffer<>(METRICS_HISTORY_SIZE));
        metricsHistory.put("completed_tasks", new CircularBuffer<>(METRICS_HISTORY_SIZE));
        metricsHistory.put("rejected_tasks", new CircularBuffer<>(METRICS_HISTORY_SIZE));
    }
    
    private void startMetricsCollection() {
        metricsCollectionTask = taskScheduler.scheduleAtFixedRate(
            this::collectMetrics,
            METRICS_COLLECTION_INTERVAL,
            METRICS_COLLECTION_INTERVAL,
            TimeUnit.SECONDS
        );
    }
    
    private void stopMetricsCollection() {
        if (metricsCollectionTask != null) {
            metricsCollectionTask.cancel(false);
        }
    }
    
    private void collectMetrics() {
        if (taskExecutor == null || taskExecutor.isShutdown()) {
            return;
        }
        
        // 收集系统指标
        double cpuUsage = getCpuUsage();
        double memoryUsage = getMemoryUsage();
        double queueUtilization = getQueueUtilization();
        
        // 收集线程池指标
        int activeThreads = taskExecutor.getActiveCount();
        long completedTasks = taskExecutor.getCompletedTaskCount();
        long rejectedTasks = taskExecutor.getRejectedExecutionCount();
        
        // 更新指标历史
        metricsHistory.get("cpu_usage").add(cpuUsage);
        metricsHistory.get("memory_usage").add(memoryUsage);
        metricsHistory.get("queue_utilization").add(queueUtilization);
        metricsHistory.get("active_threads").add((double) activeThreads);
        metricsHistory.get("completed_tasks").add((double) completedTasks);
        metricsHistory.get("rejected_tasks").add((double) rejectedTasks);
        
        // 记录线程池指标快照
        ThreadPoolMetrics metrics = new ThreadPoolMetrics(
            LocalDateTime.now(),
            cpuUsage,
            memoryUsage,
            queueUtilization,
            activeThreads,
            completedTasks,
            rejectedTasks,
            taskExecutor.getCorePoolSize(),
            taskExecutor.getMaximumPoolSize(),
            taskExecutor.getQueue().size(),
            taskExecutor.getQueue().remainingCapacity()
        );
        threadPoolMetricsHistory.add(metrics);
        
        // 检查是否需要触发告警
        checkMetricsThresholds(metrics);
    }
    
    private double getMemoryUsage() {
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        return (double) usedMemory / maxMemory * 100;
    }
    
    private void checkMetricsThresholds(ThreadPoolMetrics metrics) {
        // CPU使用率告警
        if (metrics.getCpuUsage() > CPU_THRESHOLD_HIGH) {
            sendAlert("CPU_USAGE", "CPU使用率过高",
                "WARNING", String.format("CPU使用率达到%.2f%%", metrics.getCpuUsage()),
                "SYSTEM", null);
        }
        
        // 内存使用率告警
        if (metrics.getMemoryUsage() > DEFAULT_MEMORY_LIMIT) {
            sendAlert("MEMORY_USAGE", "内存使用率过高",
                "WARNING", String.format("内存使用率达到%.2f%%", metrics.getMemoryUsage()),
                "SYSTEM", null);
        }
        
        // 队列利用率告警
        if (metrics.getQueueUtilization() > QUEUE_UTILIZATION_THRESHOLD_HIGH) {
            sendAlert("QUEUE_UTILIZATION", "任务队列利用率过高",
                "WARNING", String.format("队列利用率达到%.2f%%", metrics.getQueueUtilization() * 100),
                "SYSTEM", null);
        }
        
        // 拒绝任务告警
        if (metrics.getRejectedTasks() > 0) {
            sendAlert("REJECTED_TASKS", "任务被拒绝",
                "ERROR", String.format("已有%d个任务被拒绝", metrics.getRejectedTasks()),
                "SYSTEM", null);
        }
    }
    
    @Override
    public Map<String, Object> getThreadPoolMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // 获取最新指标
        ThreadPoolMetrics latestMetrics = threadPoolMetricsHistory.getLatest();
        if (latestMetrics != null) {
            metrics.put("timestamp", latestMetrics.getTimestamp());
            metrics.put("cpu_usage", latestMetrics.getCpuUsage());
            metrics.put("memory_usage", latestMetrics.getMemoryUsage());
            metrics.put("queue_utilization", latestMetrics.getQueueUtilization());
            metrics.put("active_threads", latestMetrics.getActiveThreads());
            metrics.put("completed_tasks", latestMetrics.getCompletedTasks());
            metrics.put("rejected_tasks", latestMetrics.getRejectedTasks());
            metrics.put("core_pool_size", latestMetrics.getCorePoolSize());
            metrics.put("max_pool_size", latestMetrics.getMaxPoolSize());
            metrics.put("queue_size", latestMetrics.getQueueSize());
            metrics.put("queue_capacity", latestMetrics.getQueueCapacity());
        }
        
        // 获取历史指标
        Map<String, List<Double>> history = new HashMap<>();
        metricsHistory.forEach((key, buffer) -> history.put(key, buffer.getAll()));
        metrics.put("history", history);
        
        return metrics;
    }
    
    private static class ThreadPoolMetrics {
        private final LocalDateTime timestamp;
        private final double cpuUsage;
        private final double memoryUsage;
        private final double queueUtilization;
        private final int activeThreads;
        private final long completedTasks;
        private final long rejectedTasks;
        private final int corePoolSize;
        private final int maxPoolSize;
        private final int queueSize;
        private final int queueCapacity;
        
        public ThreadPoolMetrics(LocalDateTime timestamp, double cpuUsage, double memoryUsage,
                               double queueUtilization, int activeThreads, long completedTasks,
                               long rejectedTasks, int corePoolSize, int maxPoolSize,
                               int queueSize, int queueCapacity) {
            this.timestamp = timestamp;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.queueUtilization = queueUtilization;
            this.activeThreads = activeThreads;
            this.completedTasks = completedTasks;
            this.rejectedTasks = rejectedTasks;
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.queueSize = queueSize;
            this.queueCapacity = queueCapacity;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public double getCpuUsage() { return cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public double getQueueUtilization() { return queueUtilization; }
        public int getActiveThreads() { return activeThreads; }
        public long getCompletedTasks() { return completedTasks; }
        public long getRejectedTasks() { return rejectedTasks; }
        public int getCorePoolSize() { return corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getQueueSize() { return queueSize; }
        public int getQueueCapacity() { return queueCapacity; }
    }
    
    private static class CircularBuffer<T> {
        private final T[] buffer;
        private final int capacity;
        private int size;
        private int head;
        private int tail;
        
        @SuppressWarnings("unchecked")
        public CircularBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = (T[]) new Object[capacity];
            this.size = 0;
            this.head = 0;
            this.tail = 0;
        }
        
        public synchronized void add(T element) {
            buffer[tail] = element;
            tail = (tail + 1) % capacity;
            if (size < capacity) {
                size++;
            } else {
                head = (head + 1) % capacity;
            }
        }
        
        public synchronized List<T> getAll() {
            List<T> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                result.add(buffer[(head + i) % capacity]);
            }
            return result;
        }
        
        public synchronized T getLatest() {
            if (size == 0) {
                return null;
            }
            return buffer[(tail - 1 + capacity) % capacity];
        }
    }
    
    private void startDynamicAdjustment() {
        dynamicAdjustmentTask = taskScheduler.scheduleAtFixedRate(
            this::adjustThreadPool,
            ADJUSTMENT_INTERVAL,
            ADJUSTMENT_INTERVAL,
            TimeUnit.SECONDS
        );
    }
    
    private void stopDynamicAdjustment() {
        if (dynamicAdjustmentTask != null) {
            dynamicAdjustmentTask.cancel(false);
        }
    }
    
    private void adjustThreadPool() {
        synchronized (executorLock) {
            if (taskExecutor == null || taskExecutor.isShutdown()) {
                return;
            }
            
            // 获取当前系统状态
            double cpuUsage = getCpuUsage();
            double queueUtilization = getQueueUtilization();
            int activeThreads = taskExecutor.getActiveCount();
            int corePoolSize = taskExecutor.getCorePoolSize();
            int maxPoolSize = taskExecutor.getMaximumPoolSize();
            int queueCapacity = taskExecutor.getQueue().remainingCapacity() + taskExecutor.getQueue().size();
            
            // 根据CPU使用率和队列利用率调整线程池
            if (cpuUsage > CPU_THRESHOLD_HIGH && queueUtilization < QUEUE_UTILIZATION_THRESHOLD_LOW) {
                // CPU使用率高但队列利用率低，减少线程数
                int newCorePoolSize = Math.max(MIN_CORE_POOL_SIZE, corePoolSize - ADJUSTMENT_STEP);
                int newMaxPoolSize = Math.max(newCorePoolSize + ADJUSTMENT_STEP, maxPoolSize - ADJUSTMENT_STEP);
                
                taskExecutor.setCorePoolSize(newCorePoolSize);
                taskExecutor.setMaximumPoolSize(newMaxPoolSize);
                
                log.info("Reduced thread pool size due to high CPU usage: corePoolSize={}, maxPoolSize={}",
                    newCorePoolSize, newMaxPoolSize);
            } else if (cpuUsage < CPU_THRESHOLD_LOW && queueUtilization > QUEUE_UTILIZATION_THRESHOLD_HIGH) {
                // CPU使用率低但队列利用率高，增加线程数
                int newCorePoolSize = Math.min(MAX_CORE_POOL_SIZE, corePoolSize + ADJUSTMENT_STEP);
                int newMaxPoolSize = Math.min(MAX_CORE_POOL_SIZE, maxPoolSize + ADJUSTMENT_STEP);
                
                taskExecutor.setCorePoolSize(newCorePoolSize);
                taskExecutor.setMaximumPoolSize(newMaxPoolSize);
                
                log.info("Increased thread pool size due to high queue utilization: corePoolSize={}, maxPoolSize={}",
                    newCorePoolSize, newMaxPoolSize);
            }
            
            // 根据队列利用率调整队列容量
            if (queueUtilization > QUEUE_UTILIZATION_THRESHOLD_HIGH) {
                // 队列利用率高，增加队列容量
                int newQueueCapacity = Math.min(MAX_QUEUE_CAPACITY, queueCapacity + ADJUSTMENT_STEP * 10);
                adjustQueueCapacity(newQueueCapacity);
                
                log.info("Increased queue capacity due to high utilization: newCapacity={}", newQueueCapacity);
            } else if (queueUtilization < QUEUE_UTILIZATION_THRESHOLD_LOW && queueCapacity > MIN_QUEUE_CAPACITY) {
                // 队列利用率低，减少队列容量
                int newQueueCapacity = Math.max(MIN_QUEUE_CAPACITY, queueCapacity - ADJUSTMENT_STEP * 10);
                adjustQueueCapacity(newQueueCapacity);
                
                log.info("Reduced queue capacity due to low utilization: newCapacity={}", newQueueCapacity);
            }
        }
    }
    
    private double getCpuUsage() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
        }
        return 0.0;
    }
    
    private double getQueueUtilization() {
        if (taskExecutor == null) {
            return 0.0;
        }
        BlockingQueue<Runnable> queue = taskExecutor.getQueue();
        return (double) queue.size() / (queue.size() + queue.remainingCapacity());
    }
    
    private void adjustQueueCapacity(int newCapacity) {
        BlockingQueue<Runnable> newQueue = new LinkedBlockingQueue<>(newCapacity);
        BlockingQueue<Runnable> oldQueue = taskExecutor.getQueue();
        
        // 将旧队列中的任务转移到新队列
        oldQueue.drainTo(newQueue);
        
        // 使用反射替换队列
        try {
            Field queueField = ThreadPoolExecutor.class.getDeclaredField("workQueue");
            queueField.setAccessible(true);
            queueField.set(taskExecutor, newQueue);
        } catch (Exception e) {
            log.error("Failed to update thread pool queue", e);
            throw new RuntimeException("Failed to update thread pool queue", e);
        }
    }
    
    @Override
    public void setThreadPoolConfig(int corePoolSize, int maxPoolSize, int queueCapacity) {
        synchronized (executorLock) {
            if (taskExecutor != null) {
                taskExecutor.setCorePoolSize(corePoolSize);
                taskExecutor.setMaximumPoolSize(maxPoolSize);
                
                // 如果队列容量发生变化，需要创建新的队列
                if (queueCapacity != taskExecutor.getQueue().remainingCapacity() + taskExecutor.getQueue().size()) {
                    BlockingQueue<Runnable> newQueue = new LinkedBlockingQueue<>(queueCapacity);
                    BlockingQueue<Runnable> oldQueue = taskExecutor.getQueue();
                    
                    // 将旧队列中的任务转移到新队列
                    oldQueue.drainTo(newQueue);
                    
                    // 使用反射替换队列
                    try {
                        Field queueField = ThreadPoolExecutor.class.getDeclaredField("workQueue");
                        queueField.setAccessible(true);
                        queueField.set(taskExecutor, newQueue);
                    } catch (Exception e) {
                        log.error("Failed to update thread pool queue", e);
                        throw new RuntimeException("Failed to update thread pool queue", e);
                    }
                }
                
                log.info("Thread pool configuration updated: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                    corePoolSize, maxPoolSize, queueCapacity);
            }
        }
    }
    
    @Override
    public Map<String, Object> getThreadPoolConfig() {
        Map<String, Object> config = new HashMap<>();
        if (taskExecutor != null) {
            config.put("corePoolSize", taskExecutor.getCorePoolSize());
            config.put("maxPoolSize", taskExecutor.getMaximumPoolSize());
            config.put("activeThreads", taskExecutor.getActiveCount());
            config.put("poolSize", taskExecutor.getPoolSize());
            config.put("queueSize", taskExecutor.getQueue().size());
            config.put("queueCapacity", taskExecutor.getQueue().remainingCapacity() + taskExecutor.getQueue().size());
            config.put("completedTasks", taskExecutor.getCompletedTaskCount());
            config.put("totalTasks", taskExecutor.getTaskCount());
            config.put("rejectedTasks", taskExecutor.getRejectedExecutionCount());
        }
        return config;
    }
    
    @Override
    public void setThreadPoolRejectionPolicy(String policy) {
        synchronized (executorLock) {
            if (taskExecutor != null) {
                RejectedExecutionHandler handler;
                switch (policy.toUpperCase()) {
                    case "ABORT":
                        handler = new ThreadPoolExecutor.AbortPolicy();
                        break;
                    case "CALLER_RUNS":
                        handler = new ThreadPoolExecutor.CallerRunsPolicy();
                        break;
                    case "DISCARD":
                        handler = new ThreadPoolExecutor.DiscardPolicy();
                        break;
                    case "DISCARD_OLDEST":
                        handler = new ThreadPoolExecutor.DiscardOldestPolicy();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported rejection policy: " + policy);
                }
                
                try {
                    Field handlerField = ThreadPoolExecutor.class.getDeclaredField("handler");
                    handlerField.setAccessible(true);
                    handlerField.set(taskExecutor, handler);
                    log.info("Thread pool rejection policy updated to: {}", policy);
                } catch (Exception e) {
                    log.error("Failed to update thread pool rejection policy", e);
                    throw new RuntimeException("Failed to update thread pool rejection policy", e);
                }
            }
        }
    }
    
    @Override
    public void setThreadPoolKeepAliveTime(long keepAliveTime, TimeUnit unit) {
        synchronized (executorLock) {
            if (taskExecutor != null) {
                taskExecutor.setKeepAliveTime(keepAliveTime, unit);
                log.info("Thread pool keep-alive time updated to: {} {}", keepAliveTime, unit);
            }
        }
    }
    
    @Override
    public void purgeThreadPool() {
        synchronized (executorLock) {
            if (taskExecutor != null) {
                taskExecutor.purge();
                log.info("Thread pool purged");
            }
        }
    }
    
    @Override
    public boolean isThreadPoolShutdown() {
        return taskExecutor != null && taskExecutor.isShutdown();
    }
    
    @Override
    public boolean isThreadPoolTerminated() {
        return taskExecutor != null && taskExecutor.isTerminated();
    }
    
    @Override
    public void shutdownThreadPool() {
        synchronized (executorLock) {
            if (taskExecutor != null && !taskExecutor.isShutdown()) {
                taskExecutor.shutdown();
                log.info("Thread pool shutdown initiated");
            }
        }
    }
    
    @Override
    public void shutdownThreadPoolNow() {
        synchronized (executorLock) {
            if (taskExecutor != null && !taskExecutor.isShutdown()) {
                taskExecutor.shutdownNow();
                log.info("Thread pool shutdown immediately");
            }
        }
    }

    @Override
    public int getTaskExpiryDays() {
        return taskExpiryDays;
    }
    
    @Override
    public void setTaskExpiryDays(int days) {
        if (days < 1) {
            throw new IllegalArgumentException("过期时间必须大于0天");
        }
        this.taskExpiryDays = days;
    }
    
    @Override
    public boolean cleanupTask(String taskId) {
        try {
            ExportTaskResult result = getTaskResult(taskId);
            if (result == null) {
                return false;
            }
            
            // 删除Redis中的任务数据
            redisTemplate.delete(TASK_KEY_PREFIX + taskId);
            redisTemplate.delete(TASK_STATUS_KEY + taskId);
            redisTemplate.delete(TASK_RESULT_KEY + taskId);
            redisTemplate.delete(TASK_PROGRESS_KEY + taskId);
            
            // 删除导出文件
            if (result.getFilePath() != null) {
                Files.deleteIfExists(Paths.get(result.getFilePath()));
            }
            
            // 清理内存中的任务数据
            taskProgress.remove(taskId);
            runningTasks.remove(taskId);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to cleanup task: {}", taskId, e);
            return false;
        }
    }
    
    @Override
    public Map<String, Boolean> cleanupTasks(List<String> taskIds) {
        Map<String, Boolean> resultMap = new HashMap<>();
        for (String taskId : taskIds) {
            resultMap.put(taskId, cleanupTask(taskId));
        }
        return resultMap;
    }
    
    @Override
    public LocalDateTime getTaskCreateTime(String taskId) {
        ExportTaskResult result = getTaskResult(taskId);
        return result != null ? result.getCreateTime() : null;
    }
    
    @Override
    public List<String> getAllTaskIds() {
        Set<String> keys = redisTemplate.keys(TASK_KEY_PREFIX + "*");
        if (keys == null) {
            return new ArrayList<>();
        }
        return keys.stream()
            .map(key -> key.substring(TASK_KEY_PREFIX.length()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<String> getExpiringTaskIds(int daysThreshold) {
        LocalDateTime thresholdTime = LocalDateTime.now().minus(taskExpiryDays - daysThreshold, ChronoUnit.DAYS);
        return getAllTaskIds().stream()
            .filter(taskId -> {
                LocalDateTime createTime = getTaskCreateTime(taskId);
                return createTime != null && createTime.isBefore(thresholdTime);
            })
            .collect(Collectors.toList());
    }
    
    @Override
    @Scheduled(cron = "0 0 0 * * ?") // 每天凌晨执行
    public void cleanupExpiredTasks() {
        log.info("Starting cleanup of expired tasks...");
        LocalDateTime expiryTime = LocalDateTime.now().minus(taskExpiryDays, ChronoUnit.DAYS);
        
        List<String> taskIds = getAllTaskIds();
        int cleanedCount = 0;
        int failedCount = 0;
        
        for (String taskId : taskIds) {
            ExportTaskResult result = getTaskResult(taskId);
            if (result != null && result.getCreateTime().isBefore(expiryTime)) {
                if (cleanupTask(taskId)) {
                    cleanedCount++;
                } else {
                    failedCount++;
                }
            }
        }
        
        log.info("Task cleanup completed. Cleaned: {}, Failed: {}", cleanedCount, failedCount);
    }

    @Override
    public int getCurrentConcurrentTasks() {
        return runningTasks.size();
    }
    
    @Override
    public int getMaxConcurrentTasks() {
        return MAX_CONCURRENT_TASKS;
    }
    
    @Override
    public void setMaxConcurrentTasks(int maxTasks) {
        if (maxTasks < 1) {
            throw new IllegalArgumentException("最大并发任务数必须大于0");
        }
        
        synchronized (queueLock) {
            if (maxTasks != MAX_CONCURRENT_TASKS) {
                // 创建新的线程池
                ExecutorService newExecutor = Executors.newFixedThreadPool(maxTasks);
                
                // 等待当前任务完成
                taskExecutorService.shutdown();
                try {
                    if (!taskExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                        taskExecutorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    taskExecutorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                
                // 更新线程池
                taskExecutorService = newExecutor;
            }
        }
    }
    
    @Override
    public int getPendingTasksCount() {
        return (int) getAllTaskIds().stream()
            .filter(taskId -> getTaskStatus(taskId) == ExportTaskStatus.PENDING)
            .count();
    }
    
    @Override
    public Map<String, Object> getQueueStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("currentConcurrentTasks", getCurrentConcurrentTasks());
        status.put("maxConcurrentTasks", getMaxConcurrentTasks());
        status.put("pendingTasks", getPendingTasksCount());
        status.put("queuePaused", isQueuePaused());
        status.put("threadPoolStatus", getThreadPoolStatus());
        return status;
    }
    
    @Override
    public void pauseQueue() {
        synchronized (queueLock) {
            queuePaused = true;
            log.info("Export task queue paused");
        }
    }
    
    @Override
    public void resumeQueue() {
        synchronized (queueLock) {
            queuePaused = false;
            queueLock.notifyAll();
            log.info("Export task queue resumed");
        }
    }
    
    @Override
    public boolean isQueuePaused() {
        return queuePaused;
    }
    
    @Override
    public Map<String, Object> getThreadPoolStatus() {
        Map<String, Object> status = new HashMap<>();
        if (taskExecutorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) taskExecutorService;
            status.put("activeThreads", executor.getActiveCount());
            status.put("poolSize", executor.getPoolSize());
            status.put("corePoolSize", executor.getCorePoolSize());
            status.put("maximumPoolSize", executor.getMaximumPoolSize());
            status.put("queueSize", executor.getQueue().size());
            status.put("completedTasks", executor.getCompletedTaskCount());
        }
        return status;
    }
    
    @Override
    public String createTask(ExportTaskType taskType, ExportTaskParams params) {
        String taskId = UUID.randomUUID().toString();
        ExportTaskResult result = new ExportTaskResult();
        result.setTaskId(taskId);
        result.setTaskType(taskType);
        result.setStatus(ExportTaskStatus.PENDING);
        result.setCreateTime(LocalDateTime.now());
        
        // 初始化任务进度
        ExportProgress progress = new ExportProgress();
        progress.setTaskId(taskId);
        progress.setTotalSteps(getTotalSteps(taskType));
        progress.setCompletedSteps(0);
        progress.setCurrentStep("初始化任务");
        progress.setProgress(0.0);
        progress.setStartTime(LocalDateTime.now());
        progress.setLastUpdateTime(LocalDateTime.now());
        progress.setStatus("PENDING");
        
        // 保存任务信息到Redis
        redisTemplate.opsForValue().set(TASK_KEY_PREFIX + taskId, taskId);
        redisTemplate.opsForValue().set(TASK_STATUS_KEY + taskId, ExportTaskStatus.PENDING.name());
        redisTemplate.opsForHash().putAll(TASK_RESULT_KEY + taskId, convertResultToMap(result));
        redisTemplate.opsForHash().putAll(TASK_PROGRESS_KEY + taskId, convertProgressToMap(progress));
        redisTemplate.opsForValue().set(TASK_PRIORITY_KEY + taskId, params.getPriority().name());
        
        taskProgress.put(taskId, progress);
        
        // 重新排序任务队列
        reorderTaskQueue();
        
        // 异步执行任务
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // 检查队列是否暂停
                synchronized (queueLock) {
                    while (queuePaused) {
                        try {
                            queueLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Task execution interrupted", e);
                        }
                    }
                }
                
                executeTask(taskId, taskType, params);
            } catch (Exception e) {
                log.error("Task execution failed", e);
                updateTaskStatus(taskId, ExportTaskStatus.FAILED, e.getMessage());
                progress.setError(e.getMessage());
                redisTemplate.opsForHash().putAll(TASK_PROGRESS_KEY + taskId, convertProgressToMap(progress));
            }
        }, taskExecutorService);
        
        runningTasks.put(taskId, future);
        return taskId;
    }

    @Override
    public ExportTaskStatus getTaskStatus(String taskId) {
        String status = redisTemplate.opsForValue().get(TASK_STATUS_KEY + taskId);
        return status != null ? ExportTaskStatus.valueOf(status) : null;
    }

    @Override
    public ExportTaskResult getTaskResult(String taskId) {
        Map<Object, Object> resultMap = redisTemplate.opsForHash().entries(TASK_RESULT_KEY + taskId);
        return convertMapToResult(resultMap);
    }
    
    @Override
    public ExportProgress getTaskProgress(String taskId) {
        Map<Object, Object> progressMap = redisTemplate.opsForHash().entries(TASK_PROGRESS_KEY + taskId);
        return convertMapToProgress(progressMap);
    }
    
    @Override
    public void updateTaskProgress(String taskId, int completedSteps, String currentStep) {
        ExportProgress progress = taskProgress.get(taskId);
        if (progress != null) {
            progress.updateProgress(completedSteps, currentStep);
            redisTemplate.opsForHash().putAll(TASK_PROGRESS_KEY + taskId, convertProgressToMap(progress));
        }
    }

    @Override
    public boolean cancelTask(String taskId) {
        CompletableFuture<Void> future = runningTasks.get(taskId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            updateTaskStatus(taskId, ExportTaskStatus.CANCELLED, "Task cancelled by user");
            
            ExportProgress progress = taskProgress.get(taskId);
            if (progress != null) {
                progress.setStatus("CANCELLED");
                redisTemplate.opsForHash().putAll(TASK_PROGRESS_KEY + taskId, convertProgressToMap(progress));
            }
            
            return true;
        }
        return false;
    }

    @Override
    public int getTaskRetryCount(String taskId) {
        String count = redisTemplate.opsForValue().get(TASK_RETRY_COUNT_KEY + taskId);
        return count != null ? Integer.parseInt(count) : 0;
    }
    
    @Override
    public int getMaxRetryCount() {
        return maxRetries;
    }
    
    @Override
    public void setMaxRetryCount(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("最大重试次数不能为负数");
        }
        this.maxRetries = maxRetries;
    }
    
    @Override
    public int getRetryInterval() {
        return retryInterval;
    }
    
    @Override
    public void setRetryInterval(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("重试间隔不能为负数");
        }
        this.retryInterval = seconds;
    }
    
    @Override
    public boolean retryTask(String taskId) {
        ExportTaskStatus status = getTaskStatus(taskId);
        if (status != ExportTaskStatus.FAILED) {
            return false;
        }
        
        int retryCount = getTaskRetryCount(taskId);
        if (retryCount >= maxRetries) {
            log.warn("Task {} has reached maximum retry count", taskId);
            return false;
        }
        
        // 更新重试次数
        redisTemplate.opsForValue().increment(TASK_RETRY_COUNT_KEY + taskId);
        
        // 记录重试历史
        Map<String, Object> retryRecord = new HashMap<>();
        retryRecord.put("timestamp", LocalDateTime.now().toString());
        retryRecord.put("retryCount", retryCount + 1);
        redisTemplate.opsForList().rightPush(TASK_RETRY_HISTORY_KEY + taskId, retryRecord);
        
        // 重置任务状态
        ExportTaskResult result = getTaskResult(taskId);
        result.setStatus(ExportTaskStatus.PENDING);
        result.setErrorMessage(null);
        redisTemplate.opsForValue().set(TASK_STATUS_KEY + taskId, ExportTaskStatus.PENDING.name());
        redisTemplate.opsForHash().putAll(TASK_RESULT_KEY + taskId, convertResultToMap(result));
        
        // 重新执行任务
        ExportProgress progress = taskProgress.get(taskId);
        if (progress != null) {
            progress.setStatus("PENDING");
            progress.setError(null);
            redisTemplate.opsForHash().putAll(TASK_PROGRESS_KEY + taskId, convertProgressToMap(progress));
        }
        
        // 重新排序任务队列
        reorderTaskQueue();
        
        return true;
    }
    
    @Override
    public Map<String, Boolean> retryTasks(List<String> taskIds) {
        Map<String, Boolean> resultMap = new HashMap<>();
        for (String taskId : taskIds) {
            resultMap.put(taskId, retryTask(taskId));
        }
        return resultMap;
    }
    
    @Override
    public List<String> getFailedTasks() {
        return getAllTaskIds().stream()
            .filter(taskId -> getTaskStatus(taskId) == ExportTaskStatus.FAILED)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> getTaskRetryHistory(String taskId) {
        List<Object> history = redisTemplate.opsForList().range(TASK_RETRY_HISTORY_KEY + taskId, 0, -1);
        if (history == null) {
            return new ArrayList<>();
        }
        
        return history.stream()
            .map(item -> (Map<String, Object>) item)
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> getTaskRetryStatus(String taskId) {
        Map<String, Object> status = new HashMap<>();
        status.put("retryCount", getTaskRetryCount(taskId));
        status.put("maxRetries", maxRetries);
        status.put("retryInterval", retryInterval);
        status.put("retryHistory", getTaskRetryHistory(taskId));
        status.put("canRetry", getTaskStatus(taskId) == ExportTaskStatus.FAILED && 
            getTaskRetryCount(taskId) < maxRetries);
        return status;
    }

    private void executeTask(String taskId, ExportTaskType taskType, ExportTaskParams params) {
        updateTaskStatus(taskId, ExportTaskStatus.PROCESSING, null);
        ExportProgress progress = taskProgress.get(taskId);
        
        try {
            Object data = null;
            switch (taskType) {
                case SEARCH_STATISTICS:
                    progress.setCurrentStep("导出搜索统计数据");
                    updateTaskProgress(taskId, 1, "准备导出数据");
                    data = dataExportService.getSearchStatistics(params.getStartTime(), params.getEndTime());
                    break;
                case USER_BEHAVIOR:
                    progress.setCurrentStep("导出用户行为统计数据");
                    updateTaskProgress(taskId, 1, "准备导出数据");
                    data = dataExportService.getUserBehaviorStats(params.getUserIds(), params.getStartTime(), params.getEndTime());
                    break;
                case PERFORMANCE_METRICS:
                    progress.setCurrentStep("导出性能指标数据");
                    updateTaskProgress(taskId, 1, "准备导出数据");
                    data = dataExportService.getPerformanceMetrics(params.getStartTime(), params.getEndTime());
                    break;
                case HOT_KEYWORDS:
                    progress.setCurrentStep("导出热门关键词数据");
                    updateTaskProgress(taskId, 1, "准备导出数据");
                    data = dataExportService.getHotKeywords(params.getSize());
                    break;
            }
            
            // 使用格式转换器转换数据
            ExportFormat format = params.getFormat();
            ExportFormatConverter converter = getExportFormatConverter(format.name());
            if (converter == null) {
                throw new IllegalStateException("未找到格式转换器: " + format.name());
            }
            
            Map<String, Object> config = getExportFormatConfig(format.name());
            byte[] convertedData = converter.convert(data, config);
            
            // 保存转换后的数据
            String filePath = saveExportedFile(taskId, format, convertedData);
            
            ExportTaskResult result = getTaskResult(taskId);
            result.setStatus(ExportTaskStatus.COMPLETED);
            result.setFilePath(filePath);
            result.setCompleteTime(LocalDateTime.now());
            
            redisTemplate.opsForValue().set(TASK_STATUS_KEY + taskId, ExportTaskStatus.COMPLETED.name());
            redisTemplate.opsForHash().putAll(TASK_RESULT_KEY + taskId, convertResultToMap(result));
            
            progress.setCompleted();
            redisTemplate.opsForHash().putAll(TASK_PROGRESS_KEY + taskId, convertProgressToMap(progress));
            
            // 更新导出格式统计信息
            updateExportFormatStats(format.name(), convertedData.length);
            
            runningTasks.remove(taskId);
        } catch (Exception e) {
            log.error("Task execution failed", e);
            updateTaskStatus(taskId, ExportTaskStatus.FAILED, e.getMessage());
            progress.setError(e.getMessage());
            redisTemplate.opsForHash().putAll(TASK_PROGRESS_KEY + taskId, convertProgressToMap(progress));
            
            // 检查是否需要自动重试
            int retryCount = getTaskRetryCount(taskId);
            if (retryCount < maxRetries) {
                log.info("Scheduling retry for task {} (attempt {}/{})", taskId, retryCount + 1, maxRetries);
                scheduleRetry(taskId);
            }
            
            throw e;
        }
    }
    
    private void scheduleRetry(String taskId) {
        CompletableFuture.delayedExecutor(retryInterval, TimeUnit.SECONDS).execute(() -> {
            try {
                retryTask(taskId);
            } catch (Exception e) {
                log.error("Failed to retry task: {}", taskId, e);
            }
        });
    }
    
    private void updateTaskStatus(String taskId, ExportTaskStatus status, String errorMessage) {
        ExportTaskResult result = getTaskResult(taskId);
        result.setStatus(status);
        result.setErrorMessage(errorMessage);
        if (status == ExportTaskStatus.COMPLETED || status == ExportTaskStatus.FAILED) {
            result.setCompleteTime(LocalDateTime.now());
        }
        
        redisTemplate.opsForValue().set(TASK_STATUS_KEY + taskId, status.name());
        redisTemplate.opsForHash().putAll(TASK_RESULT_KEY + taskId, convertResultToMap(result));
    }
    
    private int getTotalSteps(ExportTaskType taskType) {
        switch (taskType) {
            case SEARCH_STATISTICS:
                return 3; // 准备数据、导出数据、完成
            case USER_BEHAVIOR:
                return 3; // 准备数据、导出数据、完成
            case PERFORMANCE_METRICS:
                return 3; // 准备数据、导出数据、完成
            case HOT_KEYWORDS:
                return 3; // 准备数据、导出数据、完成
            default:
                return 1;
        }
    }
    
    private Map<String, String> convertResultToMap(ExportTaskResult result) {
        Map<String, String> map = new HashMap<>();
        map.put("taskId", result.getTaskId());
        map.put("taskType", result.getTaskType().name());
        map.put("status", result.getStatus().name());
        map.put("filePath", result.getFilePath());
        map.put("errorMessage", result.getErrorMessage());
        map.put("createTime", result.getCreateTime().toString());
        if (result.getCompleteTime() != null) {
            map.put("completeTime", result.getCompleteTime().toString());
        }
        return map;
    }
    
    private ExportTaskResult convertMapToResult(Map<Object, Object> map) {
        if (map.isEmpty()) {
            return null;
        }
        
        ExportTaskResult result = new ExportTaskResult();
        result.setTaskId((String) map.get("taskId"));
        result.setTaskType(ExportTaskType.valueOf((String) map.get("taskType")));
        result.setStatus(ExportTaskStatus.valueOf((String) map.get("status")));
        result.setFilePath((String) map.get("filePath"));
        result.setErrorMessage((String) map.get("errorMessage"));
        result.setCreateTime(LocalDateTime.parse((String) map.get("createTime")));
        if (map.get("completeTime") != null) {
            result.setCompleteTime(LocalDateTime.parse((String) map.get("completeTime")));
        }
        return result;
    }
    
    private Map<String, String> convertProgressToMap(ExportProgress progress) {
        Map<String, String> map = new HashMap<>();
        map.put("taskId", progress.getTaskId());
        map.put("totalSteps", String.valueOf(progress.getTotalSteps()));
        map.put("completedSteps", String.valueOf(progress.getCompletedSteps()));
        map.put("currentStep", progress.getCurrentStep());
        map.put("progress", String.valueOf(progress.getProgress()));
        map.put("startTime", progress.getStartTime().toString());
        map.put("lastUpdateTime", progress.getLastUpdateTime().toString());
        map.put("status", progress.getStatus());
        map.put("errorMessage", progress.getErrorMessage());
        return map;
    }
    
    private ExportProgress convertMapToProgress(Map<Object, Object> map) {
        if (map.isEmpty()) {
            return null;
        }
        
        ExportProgress progress = new ExportProgress();
        progress.setTaskId((String) map.get("taskId"));
        progress.setTotalSteps(Integer.parseInt((String) map.get("totalSteps")));
        progress.setCompletedSteps(Integer.parseInt((String) map.get("completedSteps")));
        progress.setCurrentStep((String) map.get("currentStep"));
        progress.setProgress(Double.parseDouble((String) map.get("progress")));
        progress.setStartTime(LocalDateTime.parse((String) map.get("startTime")));
        progress.setLastUpdateTime(LocalDateTime.parse((String) map.get("lastUpdateTime")));
        progress.setStatus((String) map.get("status"));
        progress.setErrorMessage((String) map.get("errorMessage"));
        return progress;
    }

    @Override
    public List<String> createBatchTasks(List<ExportTaskType> taskTypes, List<ExportTaskParams> paramsList) {
        if (taskTypes.size() != paramsList.size()) {
            throw new IllegalArgumentException("任务类型列表和参数列表长度不匹配");
        }
        
        // 创建批处理上下文
        String batchId = UUID.randomUUID().toString();
        List<String> taskIds = new ArrayList<>();
        BatchExportContext context = new BatchExportContext(taskIds);
        batchContexts.put(batchId, context);
        
        // 将任务分组
        List<List<ExportTaskType>> taskGroups = splitIntoGroups(taskTypes, BATCH_SIZE);
        List<List<ExportTaskParams>> paramGroups = splitIntoGroups(paramsList, BATCH_SIZE);
        
        // 并行处理每个分组
        CompletableFuture<?>[] futures = new CompletableFuture[taskGroups.size()];
        for (int i = 0; i < taskGroups.size(); i++) {
            final int groupIndex = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                processTaskGroup(batchId, taskGroups.get(groupIndex), paramGroups.get(groupIndex));
            }, taskExecutorService);
        }
        
        // 等待所有分组完成
        CompletableFuture.allOf(futures)
            .orTimeout(BATCH_TIMEOUT, TimeUnit.SECONDS)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Batch processing failed: {}", batchId, ex);
                    handleBatchFailure(batchId, ex);
                } else {
                    log.info("Batch processing completed: {}", batchId);
                }
            });
        
        return taskIds;
    }
    
    private <T> List<List<T>> splitIntoGroups(List<T> items, int groupSize) {
        List<List<T>> groups = new ArrayList<>();
        for (int i = 0; i < items.size(); i += groupSize) {
            groups.add(items.subList(i, Math.min(i + groupSize, items.size())));
        }
        return groups;
    }
    
    private void processTaskGroup(String batchId, List<ExportTaskType> taskTypes, List<ExportTaskParams> paramsList) {
        BatchExportContext context = batchContexts.get(batchId);
        if (context == null) {
            return;
        }
        
        for (int i = 0; i < taskTypes.size(); i++) {
            try {
                String taskId = createTask(taskTypes.get(i), paramsList.get(i));
                context.taskIds.add(taskId);
                
                // 初始化进度
                ExportProgress progress = new ExportProgress();
                progress.setTaskId(taskId);
                progress.setTotalSteps(getTotalSteps(taskTypes.get(i)));
                progress.setCompletedSteps(0);
                progress.setCurrentStep("初始化任务");
                progress.setProgress(0.0);
                progress.setStartTime(LocalDateTime.now());
                progress.setLastUpdateTime(LocalDateTime.now());
                progress.setStatus("PENDING");
                
                context.progressMap.put(taskId, progress);
                
                // 更新Redis中的进度信息
                redisTemplate.opsForHash().putAll(TASK_PROGRESS_KEY + taskId, convertProgressToMap(progress));
                
            } catch (Exception e) {
                log.error("Failed to create task in batch: {}", batchId, e);
                context.failedCount.incrementAndGet();
                context.errorMap.put(taskTypes.get(i).name(), e.getMessage());
            }
        }
    }
    
    private void handleBatchFailure(String batchId, Throwable ex) {
        BatchExportContext context = batchContexts.get(batchId);
        if (context != null) {
            context.errorMap.put("BATCH_ERROR", ex.getMessage());
            // 取消所有未完成的任务
            context.taskIds.forEach(taskId -> {
                if (getTaskStatus(taskId) == ExportTaskStatus.PENDING) {
                    cancelTask(taskId);
                }
            });
        }
    }
    
    @Override
    public Map<String, Object> getBatchProgress(String batchId) {
        BatchExportContext context = batchContexts.get(batchId);
        if (context == null) {
            return Map.of("error", "批处理上下文不存在");
        }
        
        Map<String, Object> progress = new HashMap<>();
        progress.put("totalTasks", context.taskIds.size());
        progress.put("completedTasks", context.completedCount.get());
        progress.put("failedTasks", context.failedCount.get());
        progress.put("startTime", context.startTime);
        progress.put("duration", ChronoUnit.SECONDS.between(context.startTime, LocalDateTime.now()));
        progress.put("progress", context.progressMap);
        progress.put("errors", context.errorMap);
        
        return progress;
    }
    
    @Override
    public void updateBatchTaskProgress(String taskId, int completedSteps, String currentStep) {
        // 更新单个任务的进度
        ExportProgress progress = taskProgress.get(taskId);
        if (progress != null) {
            progress.updateProgress(completedSteps, currentStep);
            redisTemplate.opsForHash().putAll(TASK_PROGRESS_KEY + taskId, convertProgressToMap(progress));
            
            // 更新批处理上下文中的进度
            batchContexts.forEach((batchId, context) -> {
                if (context.taskIds.contains(taskId)) {
                    context.progressMap.put(taskId, progress);
                    
                    // 检查任务是否完成
                    if (progress.getProgress() >= 100.0) {
                        context.completedCount.incrementAndGet();
                    }
                }
            });
        }
    }
    
    @Override
    public Map<String, ExportTaskStatus> getBatchTaskStatus(List<String> taskIds) {
        Map<String, ExportTaskStatus> statusMap = new HashMap<>();
        for (String taskId : taskIds) {
            statusMap.put(taskId, getTaskStatus(taskId));
        }
        return statusMap;
    }
    
    @Override
    public Map<String, ExportTaskResult> getBatchTaskResults(List<String> taskIds) {
        Map<String, ExportTaskResult> resultMap = new HashMap<>();
        for (String taskId : taskIds) {
            resultMap.put(taskId, getTaskResult(taskId));
        }
        return resultMap;
    }
    
    @Override
    public Map<String, ExportProgress> getBatchTaskProgress(List<String> taskIds) {
        Map<String, ExportProgress> progressMap = new HashMap<>();
        for (String taskId : taskIds) {
            progressMap.put(taskId, getTaskProgress(taskId));
        }
        return progressMap;
    }
    
    @Override
    public Map<String, Boolean> cancelBatchTasks(List<String> taskIds) {
        Map<String, Boolean> resultMap = new HashMap<>();
        for (String taskId : taskIds) {
            resultMap.put(taskId, cancelTask(taskId));
        }
        return resultMap;
    }

    @Override
    public TaskPriority getTaskPriority(String taskId) {
        String priorityStr = redisTemplate.opsForValue().get(TASK_PRIORITY_KEY + taskId);
        return priorityStr != null ? TaskPriority.valueOf(priorityStr) : TaskPriority.NORMAL;
    }
    
    @Override
    public void setTaskPriority(String taskId, TaskPriority priority) {
        redisTemplate.opsForValue().set(TASK_PRIORITY_KEY + taskId, priority.name());
        
        // 如果任务正在等待执行，重新排序任务队列
        ExportTaskStatus status = getTaskStatus(taskId);
        if (status == ExportTaskStatus.PENDING) {
            reorderTaskQueue();
        }
    }
    
    @Override
    public Map<String, Boolean> setBatchTaskPriority(List<String> taskIds, TaskPriority priority) {
        Map<String, Boolean> resultMap = new HashMap<>();
        for (String taskId : taskIds) {
            try {
                setTaskPriority(taskId, priority);
                resultMap.put(taskId, true);
            } catch (Exception e) {
                log.error("Failed to set priority for task: {}", taskId, e);
                resultMap.put(taskId, false);
            }
        }
        return resultMap;
    }
    
    @Override
    public List<String> getTasksByPriority(TaskPriority priority) {
        return getAllTaskIds().stream()
            .filter(taskId -> getTaskPriority(taskId) == priority)
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<TaskPriority, List<String>> getTasksGroupedByPriority() {
        Map<TaskPriority, List<String>> groupedTasks = new EnumMap<>(TaskPriority.class);
        for (TaskPriority priority : TaskPriority.values()) {
            groupedTasks.put(priority, new ArrayList<>());
        }
        
        getAllTaskIds().forEach(taskId -> {
            TaskPriority priority = getTaskPriority(taskId);
            groupedTasks.get(priority).add(taskId);
        });
        
        return groupedTasks;
    }
    
    private void reorderTaskQueue() {
        List<String> pendingTasks = getAllTaskIds().stream()
            .filter(taskId -> getTaskStatus(taskId) == ExportTaskStatus.PENDING)
            .collect(Collectors.toList());
        
        // 根据调度策略排序
        pendingTasks.sort((taskId1, taskId2) -> {
            SchedulingStrategy strategy1 = getTaskSchedulingStrategy(taskId1);
            SchedulingStrategy strategy2 = getTaskSchedulingStrategy(taskId2);
            
            if (strategy1 != strategy2) {
                return strategy1.ordinal() - strategy2.ordinal();
            }
            
            switch (strategy1) {
                case PRIORITY:
                    return Integer.compare(
                        getTaskPriority(taskId2).getValue(),
                        getTaskPriority(taskId1).getValue()
                    );
                case RESOURCE_EFFICIENT:
                    return Double.compare(
                        getTaskResourceUsage(taskId1).getOrDefault("cpuUsage", 0.0),
                        getTaskResourceUsage(taskId2).getOrDefault("cpuUsage", 0.0)
                    );
                case DEPENDENCY_BASED:
                    return Integer.compare(
                        getTaskDependencies(taskId1).size(),
                        getTaskDependencies(taskId2).size()
                    );
                case HYBRID:
                    return compareHybridStrategy(taskId1, taskId2);
                default:
                    return 0;
            }
        });
        
        // 更新任务执行顺序
        for (int i = 0; i < pendingTasks.size(); i++) {
            String taskId = pendingTasks.get(i);
            redisTemplate.opsForValue().set(TASK_KEY_PREFIX + taskId + ":order", String.valueOf(i));
        }
    }
    
    private int compareHybridStrategy(String taskId1, String taskId2) {
        // 混合策略比较：优先级(40%) + 资源效率(30%) + 依赖关系(30%)
        double score1 = calculateHybridScore(taskId1);
        double score2 = calculateHybridScore(taskId2);
        return Double.compare(score2, score1);
    }
    
    private double calculateHybridScore(String taskId) {
        double priorityScore = getTaskPriority(taskId).getValue() * 0.4;
        double resourceScore = (1 - getTaskResourceUsage(taskId).getOrDefault("cpuUsage", 0.0)) * 0.3;
        double dependencyScore = (1 - getTaskDependencies(taskId).size() * 0.1) * 0.3;
        return priorityScore + resourceScore + dependencyScore;
    }

    @Override
    public Map<String, Object> getTaskResourceLimits() {
        Map<Object, Object> limits = redisTemplate.opsForHash().entries(TASK_RESOURCE_LIMITS_KEY);
        if (limits.isEmpty()) {
            // 返回默认限制
            Map<String, Object> defaultLimits = new HashMap<>();
            defaultLimits.put("cpuLimit", DEFAULT_CPU_LIMIT);
            defaultLimits.put("memoryLimit", DEFAULT_MEMORY_LIMIT);
            defaultLimits.put("threadLimit", DEFAULT_THREAD_LIMIT);
            defaultLimits.put("diskSpaceLimit", DEFAULT_DISK_SPACE_LIMIT);
            return defaultLimits;
        }
        return convertMap(limits);
    }
    
    @Override
    public void setTaskResourceLimits(Map<String, Object> limits) {
        redisTemplate.opsForHash().putAll(TASK_RESOURCE_LIMITS_KEY, limits);
        // 检查当前运行的任务是否超过新的限制
        pauseExceedingTasks();
    }
    
    @Override
    public Map<String, Object> getTaskResourceUsage(String taskId) {
        Map<Object, Object> usage = redisTemplate.opsForHash().entries(TASK_RESOURCE_USAGE_KEY + taskId);
        if (usage.isEmpty()) {
            return new HashMap<>();
        }
        return convertMap(usage);
    }
    
    @Override
    public Map<String, Map<String, Object>> getAllTasksResourceUsage() {
        Map<String, Map<String, Object>> allUsage = new HashMap<>();
        getAllTaskIds().forEach(taskId -> {
            allUsage.put(taskId, getTaskResourceUsage(taskId));
        });
        return allUsage;
    }
    
    @Override
    public Map<String, Object> getSystemResourceUsage() {
        Map<String, Object> usage = new HashMap<>();
        
        // CPU使用率
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            usage.put("cpuUsage", sunOsBean.getSystemCpuLoad() * 100);
        }
        
        // 内存使用率
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        usage.put("memoryUsage", (double) usedMemory / maxMemory * 100);
        
        // 线程数
        usage.put("threadCount", totalThreadCount.get());
        
        // 磁盘空间
        File exportDir = new File("exports");
        if (exportDir.exists()) {
            long totalSpace = exportDir.getTotalSpace();
            long freeSpace = exportDir.getFreeSpace();
            usage.put("diskSpaceUsage", (double) (totalSpace - freeSpace) / totalSpace * 100);
        }
        
        return usage;
    }
    
    @Override
    public boolean isTaskExceedingLimits(String taskId) {
        Map<String, Object> limits = getTaskResourceLimits();
        Map<String, Object> usage = getTaskResourceUsage(taskId);
        Map<String, Object> systemUsage = getSystemResourceUsage();
        
        // 检查CPU使用率
        double cpuUsage = (double) systemUsage.get("cpuUsage");
        double cpuLimit = (double) limits.get("cpuLimit");
        if (cpuUsage > cpuLimit) {
            return true;
        }
        
        // 检查内存使用率
        double memoryUsage = (double) systemUsage.get("memoryUsage");
        double memoryLimit = (double) limits.get("memoryLimit");
        if (memoryUsage > memoryLimit) {
            return true;
        }
        
        // 检查线程数
        int threadCount = totalThreadCount.get();
        int threadLimit = (int) limits.get("threadLimit");
        if (threadCount > threadLimit) {
            return true;
        }
        
        // 检查磁盘空间
        double diskSpaceUsage = (double) systemUsage.get("diskSpaceUsage");
        double diskSpaceLimit = (double) limits.get("diskSpaceLimit");
        if (diskSpaceUsage > diskSpaceLimit) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void pauseExceedingTasks() {
        getAllTaskIds().forEach(taskId -> {
            if (isTaskExceedingLimits(taskId)) {
                pauseTask(taskId);
            }
        });
    }
    
    @Override
    public void resumePausedTasks() {
        getPausedTasks().forEach(taskId -> {
            if (!isTaskExceedingLimits(taskId)) {
                resumeTask(taskId);
            }
        });
    }
    
    @Override
    public List<String> getPausedTasks() {
        Set<String> keys = redisTemplate.keys(TASK_PAUSED_KEY + "*");
        if (keys == null) {
            return new ArrayList<>();
        }
        return keys.stream()
            .map(key -> key.substring(TASK_PAUSED_KEY.length()))
            .collect(Collectors.toList());
    }
    
    @Override
    public void setTaskPriorityWithResourceCheck(String taskId, TaskPriority priority) {
        // 如果任务优先级提高，检查是否可以恢复执行
        TaskPriority currentPriority = getTaskPriority(taskId);
        if (priority.getValue() > currentPriority.getValue()) {
            if (isTaskPaused(taskId) && !isTaskExceedingLimits(taskId)) {
                resumeTask(taskId);
            }
        }
        
        setTaskPriority(taskId, priority);
    }
    
    private void pauseTask(String taskId) {
        if (!isTaskPaused(taskId)) {
            redisTemplate.opsForValue().set(TASK_PAUSED_KEY + taskId, "true");
            CompletableFuture<Void> future = runningTasks.get(taskId);
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
            log.info("Task {} paused due to resource limits", taskId);
        }
    }
    
    private void resumeTask(String taskId) {
        if (isTaskPaused(taskId)) {
            redisTemplate.delete(TASK_PAUSED_KEY + taskId);
            ExportTaskStatus status = getTaskStatus(taskId);
            if (status == ExportTaskStatus.PENDING) {
                // 重新执行任务
                ExportTaskResult result = getTaskResult(taskId);
                ExportTaskParams params = new ExportTaskParams();
                // 设置任务参数...
                executeTask(taskId, result.getTaskType(), params);
            }
            log.info("Task {} resumed", taskId);
        }
    }
    
    private boolean isTaskPaused(String taskId) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().get(TASK_PAUSED_KEY + taskId));
    }
    
    private Map<String, Object> convertMap(Map<Object, Object> map) {
        Map<String, Object> result = new HashMap<>();
        map.forEach((key, value) -> result.put(key.toString(), value));
        return result;
    }

    @Override
    public Map<String, Object> getTaskMetrics(String taskId) {
        Map<String, Object> metrics = new HashMap<>();
        
        // 基本指标
        ExportTaskResult result = getTaskResult(taskId);
        ExportProgress progress = getTaskProgress(taskId);
        metrics.put("status", result.getStatus().name());
        metrics.put("progress", progress.getProgress());
        metrics.put("errorMessage", result.getErrorMessage());
        
        // 执行时间
        AtomicLong executionTime = taskExecutionTime.get(taskId);
        metrics.put("executionTime", executionTime != null ? executionTime.get() : 0);
        
        // 错误次数
        AtomicLong errorCount = taskErrorCount.get(taskId);
        metrics.put("errorCount", errorCount != null ? errorCount.get() : 0);
        
        // 重试次数
        AtomicLong retryCount = taskRetryCount.get(taskId);
        metrics.put("retryCount", retryCount != null ? retryCount.get() : 0);
        
        // 资源使用情况
        metrics.put("resourceUsage", getTaskResourceUsage(taskId));
        
        return metrics;
    }
    
    @Override
    public Map<String, Map<String, Object>> getAllTasksMetrics() {
        Map<String, Map<String, Object>> allMetrics = new HashMap<>();
        getAllTaskIds().forEach(taskId -> {
            allMetrics.put(taskId, getTaskMetrics(taskId));
        });
        return allMetrics;
    }
    
    @Override
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // 系统资源使用情况
        metrics.put("resourceUsage", getSystemResourceUsage());
        
        // 任务统计
        List<String> allTasks = getAllTaskIds();
        metrics.put("totalTasks", allTasks.size());
        metrics.put("completedTasks", allTasks.stream()
            .filter(taskId -> getTaskStatus(taskId) == ExportTaskStatus.COMPLETED)
            .count());
        metrics.put("failedTasks", allTasks.stream()
            .filter(taskId -> getTaskStatus(taskId) == ExportTaskStatus.FAILED)
            .count());
        metrics.put("pendingTasks", allTasks.stream()
            .filter(taskId -> getTaskStatus(taskId) == ExportTaskStatus.PENDING)
            .count());
        
        // 告警统计
        metrics.put("alertStatistics", getAlertStatistics());
        
        return metrics;
    }
    
    @Override
    public List<Map<String, Object>> getAlertRules() {
        List<Object> rules = redisTemplate.opsForList().range(ALERT_RULES_KEY, 0, -1);
        if (rules == null) {
            return new ArrayList<>();
        }
        return rules.stream()
            .map(rule -> (Map<String, Object>) rule)
            .collect(Collectors.toList());
    }
    
    @Override
    public void addAlertRule(Map<String, Object> rule) {
        rule.put("id", UUID.randomUUID().toString());
        rule.put("createTime", LocalDateTime.now().toString());
        redisTemplate.opsForList().rightPush(ALERT_RULES_KEY, rule);
    }
    
    @Override
    public void updateAlertRule(String ruleId, Map<String, Object> rule) {
        List<Map<String, Object>> rules = getAlertRules();
        for (int i = 0; i < rules.size(); i++) {
            if (ruleId.equals(rules.get(i).get("id"))) {
                rule.put("id", ruleId);
                rule.put("updateTime", LocalDateTime.now().toString());
                redisTemplate.opsForList().set(ALERT_RULES_KEY, i, rule);
                break;
            }
        }
    }
    
    @Override
    public void deleteAlertRule(String ruleId) {
        List<Map<String, Object>> rules = getAlertRules();
        for (int i = 0; i < rules.size(); i++) {
            if (ruleId.equals(rules.get(i).get("id"))) {
                redisTemplate.opsForList().remove(ALERT_RULES_KEY, 1, rules.get(i));
                break;
            }
        }
    }
    
    @Override
    public List<Map<String, Object>> getAlertHistory(String taskId) {
        List<Object> history = redisTemplate.opsForList().range(ALERT_HISTORY_KEY + taskId, 0, -1);
        if (history == null) {
            return new ArrayList<>();
        }
        return history.stream()
            .map(record -> (Map<String, Object>) record)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> getAllAlertHistory() {
        List<Map<String, Object>> allHistory = new ArrayList<>();
        getAllTaskIds().forEach(taskId -> {
            allHistory.addAll(getAlertHistory(taskId));
        });
        return allHistory;
    }
    
    @Override
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        List<Map<String, Object>> allHistory = getAllAlertHistory();
        
        // 告警总数
        statistics.put("totalAlerts", allHistory.size());
        
        // 按告警级别统计
        Map<String, Long> levelCount = allHistory.stream()
            .collect(Collectors.groupingBy(
                record -> (String) record.get("level"),
                Collectors.counting()
            ));
        statistics.put("alertsByLevel", levelCount);
        
        // 按告警类型统计
        Map<String, Long> typeCount = allHistory.stream()
            .collect(Collectors.groupingBy(
                record -> (String) record.get("type"),
                Collectors.counting()
            ));
        statistics.put("alertsByType", typeCount);
        
        // 最近24小时告警数
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        long recentAlerts = allHistory.stream()
            .filter(record -> LocalDateTime.parse((String) record.get("timestamp")).isAfter(dayAgo))
            .count();
        statistics.put("recentAlerts", recentAlerts);
        
        return statistics;
    }
    
    @Override
    public void setAlertNotificationMethod(String method, Map<String, Object> config) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("method", method);
        notification.put("config", config);
        notification.put("updateTime", LocalDateTime.now().toString());
        redisTemplate.opsForValue().set(ALERT_NOTIFICATION_KEY, notification);
    }
    
    @Override
    public Map<String, Object> getAlertNotificationMethod() {
        Object notification = redisTemplate.opsForValue().get(ALERT_NOTIFICATION_KEY);
        return notification != null ? (Map<String, Object>) notification : new HashMap<>();
    }
    
    @Override
    public boolean testAlertNotification() {
        try {
            Map<String, Object> notification = getAlertNotificationMethod();
            String method = (String) notification.get("method");
            Map<String, Object> config = (Map<String, Object>) notification.get("config");
            
            // 发送测试告警
            sendAlert("TEST", "测试告警", "INFO", "这是一条测试告警消息", method, config);
            return true;
        } catch (Exception e) {
            log.error("Failed to send test alert", e);
            return false;
        }
    }
    
    private void sendAlert(String type, String title, String level, String message,
                          String method, Map<String, Object> config) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", type);
        alert.put("title", title);
        alert.put("level", level);
        alert.put("message", message);
        alert.put("timestamp", LocalDateTime.now().toString());
        
        // 根据通知方式发送告警
        switch (method) {
            case "EMAIL":
                sendEmailAlert(alert, config);
                break;
            case "SMS":
                sendSmsAlert(alert, config);
                break;
            case "WEBHOOK":
                sendWebhookAlert(alert, config);
                break;
            default:
                log.warn("Unsupported alert notification method: {}", method);
        }
        
        // 记录告警历史
        String taskId = (String) alert.get("taskId");
        if (taskId != null) {
            redisTemplate.opsForList().rightPush(ALERT_HISTORY_KEY + taskId, alert);
        }
    }
    
    private void sendEmailAlert(Map<String, Object> alert, Map<String, Object> config) {
        // TODO: 实现邮件发送
        log.info("Sending email alert: {}", alert);
    }
    
    private void sendSmsAlert(Map<String, Object> alert, Map<String, Object> config) {
        // TODO: 实现短信发送
        log.info("Sending SMS alert: {}", alert);
    }
    
    private void sendWebhookAlert(Map<String, Object> alert, Map<String, Object> config) {
        // TODO: 实现Webhook调用
        log.info("Sending webhook alert: {}", alert);
    }

    @Override
    public void setTaskSchedulingStrategy(String taskId, SchedulingStrategy strategy) {
        redisTemplate.opsForValue().set(TASK_SCHEDULING_STRATEGY_KEY + taskId, strategy.name());
        reorderTaskQueue();
    }
    
    @Override
    public SchedulingStrategy getTaskSchedulingStrategy(String taskId) {
        String strategy = redisTemplate.opsForValue().get(TASK_SCHEDULING_STRATEGY_KEY + taskId);
        return strategy != null ? SchedulingStrategy.valueOf(strategy) : SchedulingStrategy.FIFO;
    }
    
    @Override
    public void addTaskDependency(String taskId, String dependencyTaskId) {
        redisTemplate.opsForSet().add(TASK_DEPENDENCIES_KEY + taskId, dependencyTaskId);
        reorderTaskQueue();
    }
    
    @Override
    public void removeTaskDependency(String taskId, String dependencyTaskId) {
        redisTemplate.opsForSet().remove(TASK_DEPENDENCIES_KEY + taskId, dependencyTaskId);
        reorderTaskQueue();
    }
    
    @Override
    public List<String> getTaskDependencies(String taskId) {
        Set<String> dependencies = redisTemplate.opsForSet().members(TASK_DEPENDENCIES_KEY + taskId);
        return dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
    }
    
    @Override
    public List<String> getDependentTasks(String taskId) {
        List<String> allTasks = getAllTaskIds();
        return allTasks.stream()
            .filter(task -> getTaskDependencies(task).contains(taskId))
            .collect(Collectors.toList());
    }
    
    @Override
    public void scheduleTask(String taskId, String cronExpression) {
        Map<String, Object> scheduleInfo = new HashMap<>();
        scheduleInfo.put("cronExpression", cronExpression);
        scheduleInfo.put("lastExecutionTime", null);
        scheduleInfo.put("nextExecutionTime", null);
        scheduleInfo.put("status", "SCHEDULED");
        
        redisTemplate.opsForHash().putAll(TASK_SCHEDULE_KEY + taskId, scheduleInfo);
        
        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> executeScheduledTask(taskId),
            new CronTrigger(cronExpression)
        );
        
        scheduledTasks.put(taskId, future);
        updateScheduleInfo(taskId, scheduleInfo);
    }
    
    @Override
    public void cancelScheduledTask(String taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
        redisTemplate.delete(TASK_SCHEDULE_KEY + taskId);
    }
    
    @Override
    public Map<String, Object> getScheduledTaskInfo(String taskId) {
        Map<Object, Object> info = redisTemplate.opsForHash().entries(TASK_SCHEDULE_KEY + taskId);
        return convertMap(info);
    }
    
    @Override
    public List<Map<String, Object>> getAllScheduledTasks() {
        return getAllTaskIds().stream()
            .map(this::getScheduledTaskInfo)
            .filter(info -> !info.isEmpty())
            .collect(Collectors.toList());
    }
    
    @Override
    public void pauseScheduledTask(String taskId) {
        ScheduledFuture<?> future = scheduledTasks.get(taskId);
        if (future != null) {
            future.cancel(false);
            scheduledTasks.remove(taskId);
        }
        
        Map<String, Object> scheduleInfo = getScheduledTaskInfo(taskId);
        scheduleInfo.put("status", "PAUSED");
        redisTemplate.opsForHash().putAll(TASK_SCHEDULE_KEY + taskId, scheduleInfo);
    }
    
    @Override
    public void resumeScheduledTask(String taskId) {
        Map<String, Object> scheduleInfo = getScheduledTaskInfo(taskId);
        String cronExpression = (String) scheduleInfo.get("cronExpression");
        
        if (cronExpression != null) {
            scheduleTask(taskId, cronExpression);
        }
    }
    
    @Override
    public void executeScheduledTaskNow(String taskId) {
        executeScheduledTask(taskId);
    }
    
    @Override
    public Map<String, Object> getTaskSchedulingStatus(String taskId) {
        Map<String, Object> status = new HashMap<>();
        status.put("strategy", getTaskSchedulingStrategy(taskId).name());
        status.put("dependencies", getTaskDependencies(taskId));
        status.put("dependentTasks", getDependentTasks(taskId));
        status.put("scheduleInfo", getScheduledTaskInfo(taskId));
        return status;
    }
    
    @Override
    public Map<String, Map<String, Object>> getAllTasksSchedulingStatus() {
        return getAllTaskIds().stream()
            .collect(Collectors.toMap(
                taskId -> taskId,
                this::getTaskSchedulingStatus
            ));
    }
    
    private void executeScheduledTask(String taskId) {
        try {
            ExportTaskResult result = getTaskResult(taskId);
            if (result != null) {
                ExportTaskParams params = new ExportTaskParams();
                // 设置任务参数...
                executeTask(taskId, result.getTaskType(), params);
            }
        } catch (Exception e) {
            log.error("Failed to execute scheduled task: {}", taskId, e);
        } finally {
            updateScheduleInfo(taskId);
        }
    }
    
    private void updateScheduleInfo(String taskId, Map<String, Object> scheduleInfo) {
        redisTemplate.opsForHash().putAll(TASK_SCHEDULE_KEY + taskId, scheduleInfo);
    }

    private static final String CHUNK_LOG_KEY_PREFIX = "export:chunk:log:";
    private static final int MAX_CHUNK_LOG_SIZE = 1000; // 每个分片最多保存1000条日志
    
    private void logChunkEvent(String taskId, int chunkIndex, String event, String message) {
        String logKey = CHUNK_LOG_KEY_PREFIX + taskId + ":" + chunkIndex;
        Map<String, String> logEntry = new HashMap<>();
        logEntry.put("timestamp", String.valueOf(System.currentTimeMillis()));
        logEntry.put("event", event);
        logEntry.put("message", message);
        
        redisTemplate.opsForList().leftPush(logKey, JSON.toJSONString(logEntry));
        redisTemplate.opsForList().trim(logKey, 0, MAX_CHUNK_LOG_SIZE - 1);
    }
    
    private List<Map<String, String>> getChunkLogs(String taskId, int chunkIndex) {
        String logKey = CHUNK_LOG_KEY_PREFIX + taskId + ":" + chunkIndex;
        List<String> logs = redisTemplate.opsForList().range(logKey, 0, -1);
        return logs.stream()
            .map(log -> JSON.parseObject(log, Map.class))
            .collect(Collectors.toList());
    }
    
    private static final String CHUNK_PRIORITY_KEY_PREFIX = "export:chunk:priority:";
    private static final int DEFAULT_CHUNK_PRIORITY = 0;
    private static final int MAX_CHUNK_PRIORITY = 10;
    private static final int MIN_CHUNK_PRIORITY = -10;

    @Override
    public void setChunkPriority(String taskId, int chunkIndex, int priority) {
        if (priority < MIN_CHUNK_PRIORITY || priority > MAX_CHUNK_PRIORITY) {
            throw new IllegalArgumentException("分片优先级必须在 " + MIN_CHUNK_PRIORITY + " 到 " + MAX_CHUNK_PRIORITY + " 之间");
        }
        String key = CHUNK_PRIORITY_KEY_PREFIX + taskId + ":" + chunkIndex;
        redisTemplate.opsForValue().set(key, String.valueOf(priority));
        log.info("设置分片优先级 - 任务ID: {}, 分片索引: {}, 优先级: {}", taskId, chunkIndex, priority);
    }

    @Override
    public int getChunkPriority(String taskId, int chunkIndex) {
        String key = CHUNK_PRIORITY_KEY_PREFIX + taskId + ":" + chunkIndex;
        String priority = redisTemplate.opsForValue().get(key);
        return priority != null ? Integer.parseInt(priority) : DEFAULT_CHUNK_PRIORITY;
    }

    @Override
    public Map<Integer, Boolean> setBatchChunkPriority(String taskId, Map<Integer, Integer> chunkPriorities) {
        Map<Integer, Boolean> results = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : chunkPriorities.entrySet()) {
            try {
                setChunkPriority(taskId, entry.getKey(), entry.getValue());
                results.put(entry.getKey(), true);
            } catch (Exception e) {
                log.error("设置分片优先级失败 - 任务ID: {}, 分片索引: {}, 错误: {}", 
                    taskId, entry.getKey(), e.getMessage());
                results.put(entry.getKey(), false);
            }
        }
        return results;
    }

    @Override
    public Map<Integer, Integer> getAllChunkPriorities(String taskId) {
        Map<Integer, Integer> priorities = new HashMap<>();
        Set<String> keys = redisTemplate.keys(CHUNK_PRIORITY_KEY_PREFIX + taskId + ":*");
        if (keys != null) {
            for (String key : keys) {
                String chunkIndexStr = key.substring(key.lastIndexOf(":") + 1);
                try {
                    int chunkIndex = Integer.parseInt(chunkIndexStr);
                    String priorityStr = redisTemplate.opsForValue().get(key);
                    if (priorityStr != null) {
                        priorities.put(chunkIndex, Integer.parseInt(priorityStr));
                    }
                } catch (NumberFormatException e) {
                    log.warn("无效的分片索引格式: {}", chunkIndexStr);
                }
            }
        }
        return priorities;
    }

    private void processChunkWithPriority(String taskId, int chunkIndex, Object data) {
        int priority = getChunkPriority(taskId, chunkIndex);
        log.info("处理分片 - 任务ID: {}, 分片索引: {}, 优先级: {}", taskId, chunkIndex, priority);
        
        try {
            if (priority > 0) {
                processHighPriorityChunk(taskId, chunkIndex, data);
            } else if (priority < 0) {
                processLowPriorityChunk(taskId, chunkIndex, data);
            } else {
                processNormalPriorityChunk(taskId, chunkIndex, data);
            }
        } catch (Exception e) {
            log.error("分片处理失败 - 任务ID: {}, 分片索引: {}, 错误: {}", taskId, chunkIndex, e.getMessage());
            throw e;
        }
    }

    private void processHighPriorityChunk(String taskId, int chunkIndex, Object data) {
        log.info("处理高优先级分片 - 任务ID: {}, 分片索引: {}", taskId, chunkIndex);
        
        // 使用并行流处理高优先级分片
        List<Object> dataList = (List<Object>) data;
        List<CompletableFuture<Object>> futures = dataList.stream()
            .map(item -> CompletableFuture.supplyAsync(() -> {
                try {
                    return processChunkDataItem(taskId, chunkIndex, item);
                } catch (Exception e) {
                    log.error("高优先级分片数据处理失败 - 任务ID: {}, 分片索引: {}, 错误: {}", 
                        taskId, chunkIndex, e.getMessage());
                    throw new CompletionException(e);
                }
            }, taskExecutor))
            .collect(Collectors.toList());

        // 等待所有处理完成
        List<Object> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // 合并结果
        mergeChunkResults(taskId, chunkIndex, results);
    }

    private void processLowPriorityChunk(String taskId, int chunkIndex, Object data) {
        log.info("处理低优先级分片 - 任务ID: {}, 分片索引: {}", taskId, chunkIndex);
        
        // 串行处理低优先级分片
        List<Object> dataList = (List<Object>) data;
        List<Object> results = new ArrayList<>();
        
        for (Object item : dataList) {
            try {
                Object result = processChunkDataItem(taskId, chunkIndex, item);
                results.add(result);
                
                // 更新进度
                updateChunkProgress(taskId, chunkIndex, results.size(), dataList.size());
                
                // 低优先级分片处理时增加延迟
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("低优先级分片数据处理失败 - 任务ID: {}, 分片索引: {}, 错误: {}", 
                    taskId, chunkIndex, e.getMessage());
                throw new RuntimeException(e);
            }
        }

        // 合并结果
        mergeChunkResults(taskId, chunkIndex, results);
    }

    private void processNormalPriorityChunk(String taskId, int chunkIndex, Object data) {
        log.info("处理普通优先级分片 - 任务ID: {}, 分片索引: {}", taskId, chunkIndex);
        
        // 使用默认处理方式
        List<Object> dataList = (List<Object>) data;
        List<Object> results = new ArrayList<>();
        
        for (Object item : dataList) {
            try {
                Object result = processChunkDataItem(taskId, chunkIndex, item);
                results.add(result);
                
                // 更新进度
                updateChunkProgress(taskId, chunkIndex, results.size(), dataList.size());
            } catch (Exception e) {
                log.error("普通优先级分片数据处理失败 - 任务ID: {}, 分片索引: {}, 错误: {}", 
                    taskId, chunkIndex, e.getMessage());
                throw new RuntimeException(e);
            }
        }

        // 合并结果
        mergeChunkResults(taskId, chunkIndex, results);
    }

    private Object processChunkDataItem(String taskId, int chunkIndex, Object item) {
        // 记录处理开始
        logChunkEvent(taskId, chunkIndex, "PROCESS_START", "开始处理数据项");
        
        try {
            // 处理数据项
            Object result = dataExportService.processDataItem(item);
            
            // 记录处理成功
            logChunkEvent(taskId, chunkIndex, "PROCESS_SUCCESS", "数据项处理成功");
            
            return result;
        } catch (Exception e) {
            // 记录处理失败
            logChunkEvent(taskId, chunkIndex, "PROCESS_FAILED", "数据项处理失败: " + e.getMessage());
            throw e;
        }
    }

    private void mergeChunkResults(String taskId, int chunkIndex, List<Object> results) {
        // 记录合并开始
        logChunkEvent(taskId, chunkIndex, "MERGE_START", "开始合并分片结果");
        
        try {
            // 合并结果
            dataExportService.mergeResults(results);
            
            // 记录合并成功
            logChunkEvent(taskId, chunkIndex, "MERGE_SUCCESS", "分片结果合并成功");
        } catch (Exception e) {
            // 记录合并失败
            logChunkEvent(taskId, chunkIndex, "MERGE_FAILED", "分片结果合并失败: " + e.getMessage());
            throw e;
        }
    }

    private void updateChunkProgress(String taskId, int chunkIndex, int processed, int total) {
        double progress = (double) processed / total * 100;
        logChunkEvent(taskId, chunkIndex, "PROGRESS_UPDATE", 
            String.format("分片处理进度: %.2f%% (%d/%d)", progress, processed, total));
        
        // 持久化进度信息
        persistChunkProgress(taskId, chunkIndex, processed, total);
    }

    private static final String CHUNK_TIMEOUT_KEY_PREFIX = "export:chunk:timeout:";
    private static final String CHUNK_RETRY_KEY_PREFIX = "export:chunk:retry:";
    private static final String CHUNK_PROGRESS_KEY_PREFIX = "export:chunk:progress:";
    private static final long DEFAULT_CHUNK_TIMEOUT = 300000; // 5分钟
    private static final int DEFAULT_CHUNK_MAX_RETRIES = 3;
    private static final long DEFAULT_CHUNK_RETRY_DELAY = 5000; // 5秒

    private void processChunkWithTimeout(String taskId, int chunkIndex, Object data) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                processChunkWithPriority(taskId, chunkIndex, data);
            } catch (Exception e) {
                log.error("分片处理异常 - 任务ID: {}, 分片索引: {}, 错误: {}", taskId, chunkIndex, e.getMessage());
                handleChunkFailure(taskId, chunkIndex, e);
            }
        }, taskExecutor);

        try {
            future.get(DEFAULT_CHUNK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("分片处理超时 - 任务ID: {}, 分片索引: {}", taskId, chunkIndex);
            handleChunkTimeout(taskId, chunkIndex);
        } catch (Exception e) {
            log.error("分片处理失败 - 任务ID: {}, 分片索引: {}, 错误: {}", taskId, chunkIndex, e.getMessage());
            handleChunkFailure(taskId, chunkIndex, e);
        }
    }

    private void handleChunkTimeout(String taskId, int chunkIndex) {
        String retryKey = CHUNK_RETRY_KEY_PREFIX + taskId + ":" + chunkIndex;
        int retryCount = getChunkRetryCount(taskId, chunkIndex);
        
        if (retryCount < DEFAULT_CHUNK_MAX_RETRIES) {
            // 增加重试次数
            redisTemplate.opsForValue().increment(retryKey);
            
            // 记录超时事件
            logChunkEvent(taskId, chunkIndex, "TIMEOUT", 
                String.format("分片处理超时，第%d次重试", retryCount + 1));
            
            // 延迟重试
            CompletableFuture.delayedExecutor(DEFAULT_CHUNK_RETRY_DELAY, TimeUnit.MILLISECONDS)
                .execute(() -> retryChunkProcessing(taskId, chunkIndex));
        } else {
            // 达到最大重试次数，标记为失败
            logChunkEvent(taskId, chunkIndex, "FAILED", 
                "分片处理失败：达到最大重试次数");
            updateChunkStatus(taskId, chunkIndex, "FAILED");
        }
    }

    private void handleChunkFailure(String taskId, int chunkIndex, Exception e) {
        String retryKey = CHUNK_RETRY_KEY_PREFIX + taskId + ":" + chunkIndex;
        int retryCount = getChunkRetryCount(taskId, chunkIndex);
        
        if (retryCount < DEFAULT_CHUNK_MAX_RETRIES) {
            // 增加重试次数
            redisTemplate.opsForValue().increment(retryKey);
            
            // 记录失败事件
            logChunkEvent(taskId, chunkIndex, "FAILURE", 
                String.format("分片处理失败，第%d次重试，错误: %s", retryCount + 1, e.getMessage()));
            
            // 延迟重试
            CompletableFuture.delayedExecutor(DEFAULT_CHUNK_RETRY_DELAY, TimeUnit.MILLISECONDS)
                .execute(() -> retryChunkProcessing(taskId, chunkIndex));
        } else {
            // 达到最大重试次数，标记为失败
            logChunkEvent(taskId, chunkIndex, "FAILED", 
                "分片处理失败：达到最大重试次数");
            updateChunkStatus(taskId, chunkIndex, "FAILED");
        }
    }

    private void retryChunkProcessing(String taskId, int chunkIndex) {
        try {
            // 获取分片数据
            Object data = getChunkData(taskId, chunkIndex);
            if (data != null) {
                // 重新处理分片
                processChunkWithTimeout(taskId, chunkIndex, data);
            }
        } catch (Exception e) {
            log.error("分片重试处理失败 - 任务ID: {}, 分片索引: {}, 错误: {}", 
                taskId, chunkIndex, e.getMessage());
            handleChunkFailure(taskId, chunkIndex, e);
        }
    }

    private int getChunkRetryCount(String taskId, int chunkIndex) {
        String retryKey = CHUNK_RETRY_KEY_PREFIX + taskId + ":" + chunkIndex;
        String count = redisTemplate.opsForValue().get(retryKey);
        return count != null ? Integer.parseInt(count) : 0;
    }

    private void updateChunkStatus(String taskId, int chunkIndex, String status) {
        String progressKey = CHUNK_PROGRESS_KEY_PREFIX + taskId + ":" + chunkIndex;
        Map<String, String> progress = new HashMap<>();
        progress.put("status", status);
        progress.put("updateTime", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForHash().putAll(progressKey, progress);
    }

    private void persistChunkProgress(String taskId, int chunkIndex, int processed, int total) {
        String progressKey = CHUNK_PROGRESS_KEY_PREFIX + taskId + ":" + chunkIndex;
        Map<String, String> progress = new HashMap<>();
        progress.put("processed", String.valueOf(processed));
        progress.put("total", String.valueOf(total));
        progress.put("percentage", String.valueOf((double) processed / total * 100));
        progress.put("updateTime", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForHash().putAll(progressKey, progress);
    }

    private Map<String, Object> getChunkProgress(String taskId, int chunkIndex) {
        String progressKey = CHUNK_PROGRESS_KEY_PREFIX + taskId + ":" + chunkIndex;
        Map<Object, Object> progress = redisTemplate.opsForHash().entries(progressKey);
        return convertMap(progress);
    }

    private Object getChunkData(String taskId, int chunkIndex) {
        // TODO: 实现从数据源获取分片数据的逻辑
        return null;
    }

    private static final String CHUNK_METRICS_KEY_PREFIX = "export:chunk:metrics:";
    private static final String CHUNK_RESOURCE_KEY_PREFIX = "export:chunk:resource:";
    private static final String CHUNK_CACHE_KEY_PREFIX = "export:chunk:cache:";
    private static final long CHUNK_CACHE_EXPIRY = 3600; // 1小时缓存过期时间
    private static final double MAX_CPU_USAGE = 80.0; // 最大CPU使用率
    private static final double MAX_MEMORY_USAGE = 80.0; // 最大内存使用率
    private static final int MAX_THREAD_COUNT = 10; // 每个分片最大线程数

    private void processChunkWithMetrics(String taskId, int chunkIndex, Object data) {
        long startTime = System.currentTimeMillis();
        long startMemory = getCurrentMemoryUsage();
        int startThreadCount = getCurrentThreadCount();

        try {
            // 检查资源限制
            if (!checkResourceLimits()) {
                log.warn("资源使用超过限制，等待资源释放 - 任务ID: {}, 分片索引: {}", taskId, chunkIndex);
                waitForResourceRelease();
            }

            // 处理分片
            processChunkWithTimeout(taskId, chunkIndex, data);

            // 记录性能指标
            recordChunkMetrics(taskId, chunkIndex, startTime, startMemory, startThreadCount);
        } catch (Exception e) {
            log.error("分片处理失败 - 任务ID: {}, 分片索引: {}, 错误: {}", taskId, chunkIndex, e.getMessage());
            handleChunkFailure(taskId, chunkIndex, e);
        }
    }

    private void recordChunkMetrics(String taskId, int chunkIndex, long startTime, long startMemory, int startThreadCount) {
        long endTime = System.currentTimeMillis();
        long endMemory = getCurrentMemoryUsage();
        int endThreadCount = getCurrentThreadCount();

        Map<String, String> metrics = new HashMap<>();
        metrics.put("executionTime", String.valueOf(endTime - startTime));
        metrics.put("memoryUsage", String.valueOf(endMemory - startMemory));
        metrics.put("threadCount", String.valueOf(endThreadCount - startThreadCount));
        metrics.put("cpuUsage", String.valueOf(getCurrentCpuUsage()));
        metrics.put("timestamp", String.valueOf(endTime));

        String metricsKey = CHUNK_METRICS_KEY_PREFIX + taskId + ":" + chunkIndex;
        redisTemplate.opsForHash().putAll(metricsKey, metrics);
    }

    private boolean checkResourceLimits() {
        double cpuUsage = getCurrentCpuUsage();
        double memoryUsage = getCurrentMemoryUsage();
        int threadCount = getCurrentThreadCount();

        return cpuUsage < MAX_CPU_USAGE && 
               memoryUsage < MAX_MEMORY_USAGE && 
               threadCount < MAX_THREAD_COUNT;
    }

    private void waitForResourceRelease() {
        try {
            Thread.sleep(5000); // 等待5秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private double getCurrentCpuUsage() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
        }
        return 0.0;
    }

    private long getCurrentMemoryUsage() {
        return memoryBean.getHeapMemoryUsage().getUsed();
    }

    private int getCurrentThreadCount() {
        return totalThreadCount.get();
    }

    private void cacheChunkResult(String taskId, int chunkIndex, Object result) {
        String cacheKey = CHUNK_CACHE_KEY_PREFIX + taskId + ":" + chunkIndex;
        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(result), CHUNK_CACHE_EXPIRY, TimeUnit.SECONDS);
    }

    private Object getCachedChunkResult(String taskId, int chunkIndex) {
        String cacheKey = CHUNK_CACHE_KEY_PREFIX + taskId + ":" + chunkIndex;
        String cachedResult = redisTemplate.opsForValue().get(cacheKey);
        return cachedResult != null ? JSON.parseObject(cachedResult, Object.class) : null;
    }

    private Map<String, Object> getChunkMetrics(String taskId, int chunkIndex) {
        String metricsKey = CHUNK_METRICS_KEY_PREFIX + taskId + ":" + chunkIndex;
        Map<Object, Object> metrics = redisTemplate.opsForHash().entries(metricsKey);
        return convertMap(metrics);
    }

    private Map<String, Object> getChunkResourceUsage(String taskId, int chunkIndex) {
        String resourceKey = CHUNK_RESOURCE_KEY_PREFIX + taskId + ":" + chunkIndex;
        Map<Object, Object> usage = redisTemplate.opsForHash().entries(resourceKey);
        return convertMap(usage);
    }

    private void updateChunkResourceUsage(String taskId, int chunkIndex, Map<String, Object> usage) {
        String resourceKey = CHUNK_RESOURCE_KEY_PREFIX + taskId + ":" + chunkIndex;
        redisTemplate.opsForHash().putAll(resourceKey, usage);
    }

    @Override
    public Map<String, Object> getChunkProcessingStats(String taskId, int chunkIndex) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("metrics", getChunkMetrics(taskId, chunkIndex));
        stats.put("resourceUsage", getChunkResourceUsage(taskId, chunkIndex));
        stats.put("progress", getChunkProgress(taskId, chunkIndex));
        stats.put("retryCount", getChunkRetryCount(taskId, chunkIndex));
        return stats;
    }

    private static final String CHUNK_PERFORMANCE_METRICS_KEY_PREFIX = "export:chunk:performance:metrics:";
    private static final String CHUNK_PERFORMANCE_BASELINE_KEY_PREFIX = "export:chunk:performance:baseline:";
    private static final String CHUNK_PERFORMANCE_THRESHOLDS_KEY_PREFIX = "export:chunk:performance:thresholds:";
    private static final String CHUNK_PERFORMANCE_ALERTS_KEY_PREFIX = "export:chunk:performance:alerts:";
    private static final String CHUNK_PERFORMANCE_TREND_KEY_PREFIX = "export:chunk:performance:trend:";
    private static final String CHUNK_OPTIMIZATION_SUGGESTIONS_KEY_PREFIX = "export:chunk:optimization:suggestions:";

    @Override
    public Map<String, Object> getChunkPerformanceMetrics(String taskId, int chunkIndex) {
        String metricsKey = CHUNK_PERFORMANCE_METRICS_KEY_PREFIX + taskId + ":" + chunkIndex;
        Map<Object, Object> metrics = redisTemplate.opsForHash().entries(metricsKey);
        return convertMap(metrics);
    }

    @Override
    public Map<String, Object> getChunkPerformanceReport(String taskId, int chunkIndex) {
        Map<String, Object> report = new HashMap<>();
        
        // 获取性能指标
        Map<String, Object> metrics = getChunkPerformanceMetrics(taskId, chunkIndex);
        report.put("metrics", metrics);
        
        // 获取基准数据
        Map<String, Object> baseline = getChunkPerformanceBaseline(taskId, chunkIndex);
        report.put("baseline", baseline);
        
        // 计算性能偏差
        Map<String, Object> deviation = calculatePerformanceDeviation(metrics, baseline);
        report.put("deviation", deviation);
        
        // 获取告警状态
        List<Map<String, Object>> alerts = getChunkPerformanceAlerts(taskId, chunkIndex);
        report.put("alerts", alerts);
        
        // 获取优化建议
        List<Map<String, Object>> suggestions = getChunkOptimizationSuggestions(taskId, chunkIndex);
        report.put("suggestions", suggestions);
        
        return report;
    }

    @Override
    public List<Map<String, Object>> getChunkOptimizationSuggestions(String taskId, int chunkIndex) {
        String suggestionsKey = CHUNK_OPTIMIZATION_SUGGESTIONS_KEY_PREFIX + taskId + ":" + chunkIndex;
        List<Object> suggestions = redisTemplate.opsForList().range(suggestionsKey, 0, -1);
        
        if (suggestions == null) {
            return generateOptimizationSuggestions(taskId, chunkIndex);
        }
        
        return suggestions.stream()
            .map(suggestion -> (Map<String, Object>) suggestion)
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> applyOptimizationSuggestion(String taskId, int chunkIndex, String suggestionId) {
        List<Map<String, Object>> suggestions = getChunkOptimizationSuggestions(taskId, chunkIndex);
        Map<String, Object> suggestion = suggestions.stream()
            .filter(s -> suggestionId.equals(s.get("id")))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("优化建议不存在"));
        
        Map<String, Object> result = new HashMap<>();
        result.put("suggestionId", suggestionId);
        result.put("status", "APPLIED");
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            // 应用优化建议
            applySuggestionOptimization(taskId, chunkIndex, suggestion);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, List<Map<String, Object>>> getChunkPerformanceTrend(String taskId, int chunkIndex, int timeRange) {
        String trendKey = CHUNK_PERFORMANCE_TREND_KEY_PREFIX + taskId + ":" + chunkIndex;
        long startTime = System.currentTimeMillis() - (timeRange * 3600 * 1000);
        
        List<Object> trendData = redisTemplate.opsForList().range(trendKey, 0, -1);
        if (trendData == null) {
            return new HashMap<>();
        }
        
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("executionTime", new ArrayList<>());
        result.put("memoryUsage", new ArrayList<>());
        result.put("cpuUsage", new ArrayList<>());
        result.put("threadCount", new ArrayList<>());
        
        trendData.stream()
            .map(data -> (Map<String, Object>) data)
            .filter(data -> (Long) data.get("timestamp") >= startTime)
            .forEach(data -> {
                result.get("executionTime").add(createTrendPoint("executionTime", data));
                result.get("memoryUsage").add(createTrendPoint("memoryUsage", data));
                result.get("cpuUsage").add(createTrendPoint("cpuUsage", data));
                result.get("threadCount").add(createTrendPoint("threadCount", data));
            });
        
        return result;
    }

    @Override
    public void setChunkPerformanceThresholds(String taskId, int chunkIndex, Map<String, Object> thresholds) {
        String thresholdsKey = CHUNK_PERFORMANCE_THRESHOLDS_KEY_PREFIX + taskId + ":" + chunkIndex;
        redisTemplate.opsForHash().putAll(thresholdsKey, thresholds);
    }

    @Override
    public Map<String, Object> getChunkPerformanceThresholds(String taskId, int chunkIndex) {
        String thresholdsKey = CHUNK_PERFORMANCE_THRESHOLDS_KEY_PREFIX + taskId + ":" + chunkIndex;
        Map<Object, Object> thresholds = redisTemplate.opsForHash().entries(thresholdsKey);
        return convertMap(thresholds);
    }

    @Override
    public List<Map<String, Object>> getChunkPerformanceAlerts(String taskId, int chunkIndex) {
        String alertsKey = CHUNK_PERFORMANCE_ALERTS_KEY_PREFIX + taskId + ":" + chunkIndex;
        List<Object> alerts = redisTemplate.opsForList().range(alertsKey, 0, -1);
        return alerts != null ? alerts.stream()
            .map(alert -> (Map<String, Object>) alert)
            .collect(Collectors.toList()) : new ArrayList<>();
    }

    @Override
    public Map<String, Object> getChunkPerformanceBaseline(String taskId, int chunkIndex) {
        String baselineKey = CHUNK_PERFORMANCE_BASELINE_KEY_PREFIX + taskId + ":" + chunkIndex;
        Map<Object, Object> baseline = redisTemplate.opsForHash().entries(baselineKey);
        return convertMap(baseline);
    }

    @Override
    public void updateChunkPerformanceBaseline(String taskId, int chunkIndex, Map<String, Object> baseline) {
        String baselineKey = CHUNK_PERFORMANCE_BASELINE_KEY_PREFIX + taskId + ":" + chunkIndex;
        redisTemplate.opsForHash().putAll(baselineKey, baseline);
    }

    @Override
    public Map<String, Object> getChunkPerformanceComparison(String taskId, int chunkIndex, 
        String compareTaskId, int compareChunkIndex) {
        Map<String, Object> comparison = new HashMap<>();
        
        // 获取当前分片性能数据
        Map<String, Object> currentMetrics = getChunkPerformanceMetrics(taskId, chunkIndex);
        Map<String, Object> currentBaseline = getChunkPerformanceBaseline(taskId, chunkIndex);
        
        // 获取对比分片性能数据
        Map<String, Object> compareMetrics = getChunkPerformanceMetrics(compareTaskId, compareChunkIndex);
        Map<String, Object> compareBaseline = getChunkPerformanceBaseline(compareTaskId, compareChunkIndex);
        
        // 计算性能差异
        Map<String, Object> differences = calculatePerformanceDifferences(currentMetrics, compareMetrics);
        
        comparison.put("currentMetrics", currentMetrics);
        comparison.put("currentBaseline", currentBaseline);
        comparison.put("compareMetrics", compareMetrics);
        comparison.put("compareBaseline", compareBaseline);
        comparison.put("differences", differences);
        
        return comparison;
    }

    private Map<String, Object> calculatePerformanceDeviation(Map<String, Object> metrics, Map<String, Object> baseline) {
        Map<String, Object> deviation = new HashMap<>();
        
        for (String key : metrics.keySet()) {
            if (baseline.containsKey(key)) {
                double metricValue = Double.parseDouble(metrics.get(key).toString());
                double baselineValue = Double.parseDouble(baseline.get(key).toString());
                double deviationValue = ((metricValue - baselineValue) / baselineValue) * 100;
                deviation.put(key, deviationValue);
            }
        }
        
        return deviation;
    }

    private List<Map<String, Object>> generateOptimizationSuggestions(String taskId, int chunkIndex) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        Map<String, Object> metrics = getChunkPerformanceMetrics(taskId, chunkIndex);
        Map<String, Object> baseline = getChunkPerformanceBaseline(taskId, chunkIndex);
        
        // 分析性能指标并生成优化建议
        if (metrics.containsKey("executionTime")) {
            double executionTime = Double.parseDouble(metrics.get("executionTime").toString());
            double baselineTime = Double.parseDouble(baseline.get("executionTime").toString());
            
            if (executionTime > baselineTime * 1.2) {
                suggestions.add(createSuggestion(
                    "EXECUTION_TIME",
                    "执行时间过长",
                    "建议增加并行处理或优化数据处理逻辑",
                    "HIGH"
                ));
            }
        }
        
        if (metrics.containsKey("memoryUsage")) {
            double memoryUsage = Double.parseDouble(metrics.get("memoryUsage").toString());
            double baselineMemory = Double.parseDouble(baseline.get("memoryUsage").toString());
            
            if (memoryUsage > baselineMemory * 1.3) {
                suggestions.add(createSuggestion(
                    "MEMORY_USAGE",
                    "内存使用过高",
                    "建议优化内存使用或增加内存限制",
                    "HIGH"
                ));
            }
        }
        
        // 保存优化建议
        String suggestionsKey = CHUNK_OPTIMIZATION_SUGGESTIONS_KEY_PREFIX + taskId + ":" + chunkIndex;
        redisTemplate.opsForList().rightPushAll(suggestionsKey, suggestions);
        
        return suggestions;
    }

    private Map<String, Object> createSuggestion(String id, String title, String description, String priority) {
        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("id", id);
        suggestion.put("title", title);
        suggestion.put("description", description);
        suggestion.put("priority", priority);
        suggestion.put("timestamp", System.currentTimeMillis());
        return suggestion;
    }

    private void applySuggestionOptimization(String taskId, int chunkIndex, Map<String, Object> suggestion) {
        String suggestionId = (String) suggestion.get("id");
        
        switch (suggestionId) {
            case "EXECUTION_TIME":
                optimizeExecutionTime(taskId, chunkIndex);
                break;
            case "MEMORY_USAGE":
                optimizeMemoryUsage(taskId, chunkIndex);
                break;
            default:
                throw new IllegalArgumentException("不支持的优化建议类型: " + suggestionId);
        }
    }

    private void optimizeExecutionTime(String taskId, int chunkIndex) {
        // 实现执行时间优化逻辑
        // 例如：增加并行处理、优化数据处理逻辑等
    }

    private void optimizeMemoryUsage(String taskId, int chunkIndex) {
        // 实现内存使用优化逻辑
        // 例如：优化内存分配、增加内存限制等
    }

    private Map<String, Object> createTrendPoint(String metric, Map<String, Object> data) {
        Map<String, Object> point = new HashMap<>();
        point.put("timestamp", data.get("timestamp"));
        point.put("value", data.get(metric));
        return point;
    }

    private Map<String, Object> calculatePerformanceDifferences(Map<String, Object> current, Map<String, Object> compare) {
        Map<String, Object> differences = new HashMap<>();
        
        for (String key : current.keySet()) {
            if (compare.containsKey(key)) {
                double currentValue = Double.parseDouble(current.get(key).toString());
                double compareValue = Double.parseDouble(compare.get(key).toString());
                double difference = ((currentValue - compareValue) / compareValue) * 100;
                differences.put(key, difference);
            }
        }
        
        return differences;
    }
} 