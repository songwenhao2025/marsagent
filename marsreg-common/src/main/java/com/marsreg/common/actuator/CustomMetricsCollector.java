package com.marsreg.common.actuator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class CustomMetricsCollector {

    private final MeterRegistry registry;
    private Counter loginCounter;
    private Counter registerCounter;

    @PostConstruct
    public void init() {
        loginCounter = Counter.builder("marsreg.auth.login")
                .description("登录次数统计")
                .register(registry);
        
        registerCounter = Counter.builder("marsreg.auth.register")
                .description("注册次数统计")
                .register(registry);
    }

    public void incrementLoginCount() {
        loginCounter.increment();
    }

    public void incrementRegisterCount() {
        registerCounter.increment();
    }
} 