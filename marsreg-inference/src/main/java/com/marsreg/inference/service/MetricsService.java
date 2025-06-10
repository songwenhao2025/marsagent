package com.marsreg.inference.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class MetricsService {
    
    private final MeterRegistry registry;
    private final AtomicInteger contextSize = new AtomicInteger(0);
    private final AtomicInteger cacheSize = new AtomicInteger(0);
    
    private Timer inferenceTimer;
    private Timer streamInferenceTimer;
    private Counter inferenceCounter;
    private Counter streamInferenceCounter;
    private Counter errorCounter;
    private Gauge contextSizeGauge;
    private Gauge cacheSizeGauge;
    
    public void init() {
        inferenceTimer = registry.timer("inference.request.duration");
        streamInferenceTimer = registry.timer("inference.stream.duration");
        inferenceCounter = registry.counter("inference.request.count");
        streamInferenceCounter = registry.counter("inference.stream.count");
        errorCounter = registry.counter("inference.error.count");
        
        contextSizeGauge = Gauge.builder("inference.context.size", contextSize, AtomicInteger::get)
            .description("上下文大小")
            .register(registry);
            
        cacheSizeGauge = Gauge.builder("inference.cache.size", cacheSize, AtomicInteger::get)
            .description("缓存大小")
            .register(registry);
    }
    
    public Timer.Sample startInferenceTimer() {
        inferenceCounter.increment();
        return Timer.start(registry);
    }
    
    public Timer.Sample startStreamInferenceTimer() {
        streamInferenceCounter.increment();
        return Timer.start(registry);
    }
    
    public void stopTimer(Timer.Sample sample, Timer timer) {
        if (sample != null && timer != null) {
            sample.stop(timer);
        }
    }
    
    public void recordError() {
        errorCounter.increment();
    }
    
    public void updateContextSize(int size) {
        contextSize.set(size);
    }
    
    public void updateCacheSize(int size) {
        cacheSize.set(size);
    }
    
    public void incrementCacheSize() {
        cacheSize.incrementAndGet();
    }
    
    public void decrementCacheSize() {
        cacheSize.decrementAndGet();
    }
    
    public Timer getInferenceTimer() {
        return inferenceTimer;
    }
    
    public Timer getStreamInferenceTimer() {
        return streamInferenceTimer;
    }
} 