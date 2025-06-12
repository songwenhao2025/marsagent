package com.marsreg.common.model;

import com.marsreg.common.enums.DocumentStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public abstract class BaseDocument {
    private Long id;
    private String title;
    private String content;
    private String type;
    private DocumentStatus status;
    private String contentType;
    private String originalName;
    private Long size;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
    private Map<String, Object> metadata;
} 