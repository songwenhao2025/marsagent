package com.marsreg.search.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class Document {
    private String id;
    private String title;
    private String content;
    private Map<String, Object> metadata;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 