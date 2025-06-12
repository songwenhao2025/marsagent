package com.marsreg.common.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
public class BaseSearchResponse {
    private List<?> results;
    private long total;
    private int page;
    private int size;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private Map<String, Object> aggregations;
    private Map<String, List<String>> highlights;
    private long took;
    private String scrollId;

    public BaseSearchResponse() {
    }
} 