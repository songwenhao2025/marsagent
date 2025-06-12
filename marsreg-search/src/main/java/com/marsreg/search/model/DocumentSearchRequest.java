package com.marsreg.search.model;

import com.marsreg.common.model.BaseSearchRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DocumentSearchRequest extends BaseSearchRequest {
    private List<String> documentTypes;
    private List<String> statuses;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String category;
    private List<String> tags;
    private String md5;
    private String bucket;
    private String storagePath;
    private String objectName;
    private String errorMessage;
    private Boolean hasError;
} 