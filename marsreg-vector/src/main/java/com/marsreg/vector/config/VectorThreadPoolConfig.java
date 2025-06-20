package com.marsreg.vector.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
@Profile("vector")
public class VectorThreadPoolConfig {
    
    private final VectorizationConfig vectorizationConfig;
    
    @Bean("vectorizationExecutor")
    public ExecutorService vectorizationExecutor() {
        return Executors.newFixedThreadPool(vectorizationConfig.getThreadPoolSize());
    }
} 