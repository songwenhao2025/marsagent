package com.marsreg.inference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class InferenceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InferenceApplication.class, args);
    }
} 