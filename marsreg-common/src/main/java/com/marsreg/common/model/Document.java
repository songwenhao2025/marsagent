package com.marsreg.common.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class Document {
    private Long id;
    private String name;
    private String contentType;
    private String status;
    private String description;
    private String tags;
    private String customMetadata;
    private Long size;
    private String url;
    private String content;
    private String type;
    private String originalName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
    private Map<String, Object> metadata;
} 