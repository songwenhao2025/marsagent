package com.marsreg.document.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.marsreg.document.repository")
@EntityScan(basePackages = "com.marsreg.document.entity")
public class DocumentJpaConfig {
} 