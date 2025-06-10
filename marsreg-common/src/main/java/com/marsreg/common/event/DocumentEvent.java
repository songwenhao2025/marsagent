package com.marsreg.common.event;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public abstract class DocumentEvent {
    private String documentId;
    private LocalDateTime timestamp;
    private String operator;
    private String operationType;
    private String source;
    private String metadata;
    
    protected DocumentEvent() {
        this.timestamp = LocalDateTime.now();
    }
} 