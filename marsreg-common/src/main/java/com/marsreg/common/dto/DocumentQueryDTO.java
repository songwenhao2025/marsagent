package com.marsreg.common.dto;

import lombok.Data;

@Data
public class DocumentQueryDTO {
    private String keyword;
    private String contentType;
    private String status;
    private String tags;
    private String customMetadata;
} 