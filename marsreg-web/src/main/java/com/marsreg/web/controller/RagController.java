package com.marsreg.web.controller;

import com.marsreg.inference.service.InferenceService;
import com.marsreg.inference.model.InferenceRequest;
import com.marsreg.inference.model.InferenceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private InferenceService inferenceService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        InferenceRequest inferenceRequest = InferenceRequest.builder()
            .question(question)
            .searchType(InferenceRequest.SearchType.HYBRID)
            .maxDocuments(5)
            .minSimilarity(0.7f)
            .build();
        InferenceResponse response = inferenceService.infer(inferenceRequest);
        return ResponseEntity.ok(Map.of("answer", response.getAnswer()));
    }
} 