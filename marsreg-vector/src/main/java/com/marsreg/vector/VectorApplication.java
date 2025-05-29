package com.marsreg.vector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class VectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(VectorApplication.class, args);
    }
} 