package com.marsreg.inference.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Timer inferenceTimer(MeterRegistry registry) {
        return Timer.builder("inference.request.duration")
            .description("推理请求处理时间")
            .tag("type", "inference")
            .register(registry);
    }

    @Bean
    public Timer streamInferenceTimer(MeterRegistry registry) {
        return Timer.builder("inference.stream.duration")
            .description("流式推理处理时间")
            .tag("type", "stream")
            .register(registry);
    }

    @Bean
    public Counter inferenceCounter(MeterRegistry registry) {
        return Counter.builder("inference.request.count")
            .description("推理请求总数")
            .tag("type", "inference")
            .register(registry);
    }

    @Bean
    public Counter streamInferenceCounter(MeterRegistry registry) {
        return Counter.builder("inference.stream.count")
            .description("流式推理请求总数")
            .tag("type", "stream")
            .register(registry);
    }

    @Bean
    public Counter errorCounter(MeterRegistry registry) {
        return Counter.builder("inference.error.count")
            .description("推理错误总数")
            .register(registry);
    }

    @Bean
    public Gauge contextSizeGauge(MeterRegistry registry) {
        return Gauge.builder("inference.context.size", () -> 0)
            .description("上下文大小")
            .register(registry);
    }

    @Bean
    public Gauge cacheSizeGauge(MeterRegistry registry) {
        return Gauge.builder("inference.cache.size", () -> 0)
            .description("缓存大小")
            .register(registry);
    }
} 