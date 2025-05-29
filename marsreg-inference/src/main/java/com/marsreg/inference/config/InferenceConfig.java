package com.marsreg.inference.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@Validated
@EnableConfigurationProperties(InferenceProperties.class)
public class InferenceConfig {
} 