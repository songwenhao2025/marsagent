package com.marsreg.inference.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class InferenceResponse {
    private String answer;
    private List<DocumentReference> references;
    private Map<String, Object> metadata;
    
    @Data
    @Builder
    public static class DocumentReference {
        private String documentId;
        private String title;
        private String content;
        private Float relevance;
        private Map<String, Object> metadata;
    }
} 