package com.marsreg.document.dto;

import lombok.Data;

@Data
public class DocumentQueryDTO {
    private String keyword;
    private String contentType;
    private String status;
    private Long startTime;
    private Long endTime;
} 