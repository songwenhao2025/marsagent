package com.marsreg.document.alert;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadAlertService {

    private final MeterRegistry registry;
    
    @Value("${marsreg.document.alert.failure-rate-threshold:0.1}")
    private double failureRateThreshold;
    
    @Value("${marsreg.document.alert.slow-upload-threshold:5000}")
    private long slowUploadThreshold;
    
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkUploadMetrics() {
        double totalUploads = registry.get("document.upload.total").counter().count();
        double failedUploads = registry.get("document.upload.failure").counter().count();
        
        if (totalUploads > 0) {
            double failureRate = failedUploads / totalUploads;
            if (failureRate > failureRateThreshold) {
                log.error("上传失败率过高: {:.2%} (阈值: {:.2%})", failureRate, failureRateThreshold);
                // TODO: 发送告警通知
            }
        }
        
        double avgUploadTime = registry.get("document.upload.time").timer().mean(TimeUnit.MILLISECONDS);
        if (avgUploadTime > slowUploadThreshold) {
            log.warn("上传速度过慢: {}ms (阈值: {}ms)", avgUploadTime, slowUploadThreshold);
            // TODO: 发送告警通知
        }
    }
} 