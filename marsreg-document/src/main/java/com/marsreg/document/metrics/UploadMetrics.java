package com.marsreg.document.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class UploadMetrics {

    private final MeterRegistry registry;
    
    private final Counter uploadCounter;
    private final Counter uploadSuccessCounter;
    private final Counter uploadFailureCounter;
    private final Timer uploadTimer;
    private final Counter uploadSizeCounter;
    private final Counter uploadCompressionCounter;
    private final Counter uploadCompressionSizeCounter;
    private final DistributionSummary uploadSizeDistribution;
    private final Counter uploadTypeCounter;
    
    public UploadMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        this.uploadCounter = Counter.builder("document.upload.total")
            .description("Total number of upload attempts")
            .register(registry);
            
        this.uploadSuccessCounter = Counter.builder("document.upload.success")
            .description("Number of successful uploads")
            .register(registry);
            
        this.uploadFailureCounter = Counter.builder("document.upload.failure")
            .description("Number of failed uploads")
            .register(registry);
            
        this.uploadTimer = Timer.builder("document.upload.time")
            .description("Time taken for uploads")
            .register(registry);
            
        this.uploadSizeCounter = Counter.builder("document.upload.size")
            .description("Total size of uploaded files")
            .register(registry);
            
        this.uploadCompressionCounter = Counter.builder("document.upload.compression")
            .description("Number of compressed files")
            .register(registry);
            
        this.uploadCompressionSizeCounter = Counter.builder("document.upload.compression.size")
            .description("Total size reduction from compression")
            .register(registry);
            
        this.uploadSizeDistribution = DistributionSummary.builder("document.upload.size.distribution")
            .description("Distribution of uploaded file sizes")
            .baseUnit("bytes")
            .register(registry);
            
        this.uploadTypeCounter = Counter.builder("document.upload.type")
            .description("Number of uploads by file type")
            .tag("type", "unknown")
            .register(registry);
    }
    
    public void recordUploadAttempt() {
        uploadCounter.increment();
    }
    
    public void recordUploadSuccess() {
        uploadSuccessCounter.increment();
    }
    
    public void recordUploadFailure() {
        uploadFailureCounter.increment();
    }
    
    public void recordUploadTime(long timeInMillis) {
        uploadTimer.record(timeInMillis, TimeUnit.MILLISECONDS);
    }
    
    public void recordUploadSize(long bytes) {
        uploadSizeCounter.increment(bytes);
        uploadSizeDistribution.record(bytes);
    }
    
    public void recordCompression(long originalSize, long compressedSize) {
        uploadCompressionCounter.increment();
        uploadCompressionSizeCounter.increment(originalSize - compressedSize);
    }
    
    public void recordFileType(String fileType) {
        registry.counter("document.upload.type", "type", fileType).increment();
    }
} 