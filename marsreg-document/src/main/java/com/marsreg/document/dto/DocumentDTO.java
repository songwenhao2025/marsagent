package com.marsreg.document.dto;

import com.marsreg.document.enums.DocumentStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentDTO {
    private Long id;
    private String name;
    private String originalName;
    private String contentType;
    private Long size;
    private String category;
    private List<String> tags;
    private DocumentStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedTime;
    private String createdBy;
    private String updatedBy;
} 