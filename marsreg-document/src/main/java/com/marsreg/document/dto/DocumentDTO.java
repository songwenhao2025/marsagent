package com.marsreg.document.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentDTO {
    private Long id;
    private String title;
    private String description;
    private String author;
    private String category;
    private String tags;
    private String language;
    private String source;
    private String customMetadata;
    private String content;
    private List<DocumentChunkDTO> chunks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
    private String errorMessage;
} 