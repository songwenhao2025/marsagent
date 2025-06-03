package com.marsreg.document.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentDTO {
    private Long id;
    private String name;
    private String originalName;
    private String contentType;
    private Long size;
    private String storagePath;
    private String bucket;
    private String objectName;
    private String md5;
    private String status;
    private String errorMessage;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 