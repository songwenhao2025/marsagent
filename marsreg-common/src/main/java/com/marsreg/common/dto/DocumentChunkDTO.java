package com.marsreg.common.dto;

import lombok.Data;

@Data
public class DocumentChunkDTO {
    private Long id;
    private Long documentId;
    private String content;
    private Integer chunkIndex;
    private String language;
    private String metadata;
} 