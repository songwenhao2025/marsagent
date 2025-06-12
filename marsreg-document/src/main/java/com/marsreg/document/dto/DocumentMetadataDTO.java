package com.marsreg.document.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentMetadataDTO {
    private Long id;
    private String title;
    private String originalName;
    private String contentType;
    private Long size;
    private String objectName;
    private String storagePath;
    private String bucket;
    private String md5;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private String category;
    private List<String> tags;
    private String content;
    private String errorMessage;
    private LocalDateTime processedTime;
} 