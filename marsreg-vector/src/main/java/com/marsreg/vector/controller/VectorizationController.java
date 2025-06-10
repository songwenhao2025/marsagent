package com.marsreg.vector.controller;

import com.marsreg.common.annotation.Log;
import com.marsreg.common.annotation.RateLimit;
import com.marsreg.common.response.ApiResponse;
import com.marsreg.vector.service.VectorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vectorization")
@RequiredArgsConstructor
public class VectorizationController {
    
    private final VectorizationService vectorizationService;
    
    @PostMapping("/vectorize")
    @Log(module = "向量化", operation = "向量化", description = "文本向量化")
    @RateLimit(limit = 100, time = 60)
    public ApiResponse<float[]> vectorize(@RequestBody String text) {
        float[] result = vectorizationService.vectorize(text);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/batch-vectorize")
    @Log(module = "向量化", operation = "批量向量化", description = "批量文本向量化")
    @RateLimit(limit = 50, time = 60)
    public ApiResponse<List<float[]>> batchVectorize(@RequestBody List<String> texts) {
        List<float[]> result = vectorizationService.batchVectorize(texts);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/vectorize-with-cache")
    @Log(module = "向量化", operation = "缓存向量化", description = "带缓存的文本向量化")
    @RateLimit(limit = 200, time = 60)
    public ApiResponse<float[]> vectorizeWithCache(@RequestBody String text) {
        float[] result = vectorizationService.vectorizeWithCache(text);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/batch-vectorize-with-cache")
    @Log(module = "向量化", operation = "批量缓存向量化", description = "带缓存的批量文本向量化")
    @RateLimit(limit = 100, time = 60)
    public ApiResponse<List<float[]>> batchVectorizeWithCache(@RequestBody List<String> texts) {
        List<float[]> result = vectorizationService.batchVectorizeWithCache(texts);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/model-info")
    @Log(module = "向量化", operation = "获取模型信息", description = "获取向量化模型信息")
    public ApiResponse<Map<String, Object>> getModelInfo() {
        Map<String, Object> result = vectorizationService.getModelInfo();
        return ApiResponse.success(result);
    }
    
    @PostMapping("/update-model")
    @Log(module = "向量化", operation = "更新模型", description = "更新向量化模型")
    public ApiResponse<Void> updateModel(@RequestBody String modelPath) {
        vectorizationService.updateModel(modelPath);
        return ApiResponse.success(null);
    }
    
    @PostMapping("/warmup")
    @Log(module = "向量化", operation = "预热模型", description = "预热向量化模型")
    public ApiResponse<Void> warmupModel() {
        vectorizationService.warmupModel();
        return ApiResponse.success(null);
    }
}

class VectorizationRequest {
    private String text;
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}

class BatchVectorizationRequest {
    private java.util.List<String> texts;
    public java.util.List<String> getTexts() { return texts; }
    public void setTexts(java.util.List<String> texts) { this.texts = texts; }
} 