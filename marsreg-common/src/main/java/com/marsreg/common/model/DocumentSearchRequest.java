package com.marsreg.common.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Data
@Builder
public class DocumentSearchRequest {
    private String query;
    private int page;
    private int size;
    private List<String> documentTypes;
    private List<String> statuses;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String sortField;
    private String sortOrder;
    private Map<String, Object> filters;
    private boolean highlight;
    private List<String> highlightFields;
    private Float minScore;
} 