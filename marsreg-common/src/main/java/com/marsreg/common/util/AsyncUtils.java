package com.marsreg.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 异步任务工具类
 */
@Slf4j
@Component
public class AsyncUtils {

    /**
     * 执行异步任务（通用线程池）
     */
    @Async("commonExecutor")
    public <T> CompletableFuture<T> execute(Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Exception e) {
                log.error("异步任务执行失败", e);
                throw new RuntimeException("异步任务执行失败", e);
            }
        });
    }

    /**
     * 执行异步任务（计算密集型线程池）
     */
    @Async("computeExecutor")
    public <T> CompletableFuture<T> executeCompute(Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Exception e) {
                log.error("计算密集型异步任务执行失败", e);
                throw new RuntimeException("计算密集型异步任务执行失败", e);
            }
        });
    }

    /**
     * 执行异步任务（IO密集型线程池）
     */
    @Async("ioExecutor")
    public <T> CompletableFuture<T> executeIO(Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Exception e) {
                log.error("IO密集型异步任务执行失败", e);
                throw new RuntimeException("IO密集型异步任务执行失败", e);
            }
        });
    }

    /**
     * 执行异步任务（定时任务线程池）
     */
    @Async("scheduledExecutor")
    public <T> CompletableFuture<T> executeScheduled(Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Exception e) {
                log.error("定时异步任务执行失败", e);
                throw new RuntimeException("定时异步任务执行失败", e);
            }
        });
    }

    /**
     * 执行异步任务（无返回值）
     */
    @Async("commonExecutor")
    public void execute(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("异步任务执行失败", e);
            throw new RuntimeException("异步任务执行失败", e);
        }
    }

    /**
     * 执行异步任务（无返回值，计算密集型线程池）
     */
    @Async("computeExecutor")
    public void executeCompute(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("计算密集型异步任务执行失败", e);
            throw new RuntimeException("计算密集型异步任务执行失败", e);
        }
    }

    /**
     * 执行异步任务（无返回值，IO密集型线程池）
     */
    @Async("ioExecutor")
    public void executeIO(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("IO密集型异步任务执行失败", e);
            throw new RuntimeException("IO密集型异步任务执行失败", e);
        }
    }

    /**
     * 执行异步任务（无返回值，定时任务线程池）
     */
    @Async("scheduledExecutor")
    public void executeScheduled(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("定时异步任务执行失败", e);
            throw new RuntimeException("定时异步任务执行失败", e);
        }
    }
} 