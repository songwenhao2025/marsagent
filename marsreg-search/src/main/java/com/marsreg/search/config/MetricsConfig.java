package com.marsreg.search.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        return io.micrometer.core.instrument.Metrics.globalRegistry;
    }
} 