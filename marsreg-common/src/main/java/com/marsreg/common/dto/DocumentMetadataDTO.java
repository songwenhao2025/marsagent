package com.marsreg.common.dto;

import lombok.Data;

@Data
public class DocumentMetadataDTO {
    private String name;
    private String contentType;
    private String status;
    private String description;
    private String tags;
    private String customMetadata;
} 