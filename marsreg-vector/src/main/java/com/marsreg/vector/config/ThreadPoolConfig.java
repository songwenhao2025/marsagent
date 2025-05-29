package com.marsreg.vector.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
public class ThreadPoolConfig {
    
    private final VectorizationConfig vectorizationConfig;
    
    @Bean
    public ExecutorService vectorizationExecutor() {
        return Executors.newFixedThreadPool(vectorizationConfig.getThreadPoolSize());
    }
} 