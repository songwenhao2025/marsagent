package com.marsreg.search.model;

import lombok.Data;

@Data
public class TimeRangeStats {
    private String timeRange;
    private Long count;
    private Double percentage;
} 