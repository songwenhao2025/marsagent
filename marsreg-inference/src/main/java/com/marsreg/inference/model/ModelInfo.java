package com.marsreg.inference.model;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ModelInfo {
    private String id;
    private String name;
    private String version;
    private String description;
    private String provider;
    private String status;
    private Map<String, Object> parameters;
    private Map<String, Object> metrics;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    public enum Status {
        ACTIVE,
        INACTIVE,
        LOADING,
        ERROR
    }
} 