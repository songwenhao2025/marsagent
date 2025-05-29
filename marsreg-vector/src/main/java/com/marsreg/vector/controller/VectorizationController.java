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
    public ApiResponse<List<Float>> vectorize(@RequestBody String text) {
        return ApiResponse.success(vectorizationService.vectorize(text));
    }
    
    @PostMapping("/batch-vectorize")
    @Log(module = "向量化", operation = "批量向量化", description = "批量文本向量化")
    @RateLimit(limit = 50, time = 60)
    public ApiResponse<List<List<Float>>> batchVectorize(@RequestBody List<String> texts) {
        return ApiResponse.success(vectorizationService.batchVectorize(texts));
    }
    
    @PostMapping("/vectorize-with-cache")
    @Log(module = "向量化", operation = "缓存向量化", description = "带缓存的文本向量化")
    @RateLimit(limit = 200, time = 60)
    public ApiResponse<List<Float>> vectorizeWithCache(@RequestBody String text) {
        return ApiResponse.success(vectorizationService.vectorizeWithCache(text));
    }
    
    @PostMapping("/batch-vectorize-with-cache")
    @Log(module = "向量化", operation = "批量缓存向量化", description = "带缓存的批量文本向量化")
    @RateLimit(limit = 100, time = 60)
    public ApiResponse<List<List<Float>>> batchVectorizeWithCache(@RequestBody List<String> texts) {
        return ApiResponse.success(vectorizationService.batchVectorizeWithCache(texts));
    }
    
    @GetMapping("/model-info")
    @Log(module = "向量化", operation = "获取模型信息", description = "获取向量化模型信息")
    public ApiResponse<Map<String, Object>> getModelInfo() {
        return ApiResponse.success(vectorizationService.getModelInfo());
    }
    
    @PostMapping("/update-model")
    @Log(module = "向量化", operation = "更新模型", description = "更新向量化模型")
    @RateLimit(limit = 10, time = 60)
    public ApiResponse<Void> updateModel(@RequestParam String modelPath) {
        vectorizationService.updateModel(modelPath);
        return ApiResponse.success();
    }
    
    @PostMapping("/warmup")
    @Log(module = "向量化", operation = "预热模型", description = "预热向量化模型")
    @RateLimit(limit = 5, time = 60)
    public ApiResponse<Void> warmupModel() {
        vectorizationService.warmupModel();
        return ApiResponse.success();
    }
} 