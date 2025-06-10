package com.marsreg.common.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class Document {
    private String id;
    private String title;
    private String content;
    private String type;
    private String status;
    private String contentType;
    private String originalName;
    private Long size;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
    private Map<String, Object> metadata;
} 