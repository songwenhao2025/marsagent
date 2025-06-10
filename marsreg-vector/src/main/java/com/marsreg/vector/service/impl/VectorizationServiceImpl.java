package com.marsreg.vector.service.impl;

import com.marsreg.common.annotation.Cache;
import com.marsreg.common.annotation.Log;
import com.marsreg.common.exception.BusinessException;
import com.marsreg.vector.cache.VectorCacheManager;
import com.marsreg.vector.config.VectorizationConfig;
import com.marsreg.vector.model.SentenceTransformerModel;
import com.marsreg.vector.service.VectorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorizationServiceImpl implements VectorizationService {

    private final VectorizationConfig config;
    private final ExecutorService vectorizationExecutor;
    private final SentenceTransformerModel model;
    private final VectorCacheManager cacheManager;
    
    private static final int BATCH_SIZE = 32; // 批处理大小
    
    @Override
    @Log(module = "向量化", operation = "向量化", description = "文本向量化")
    public float[] vectorize(String text) {
        float[] vector = cacheManager.getVector(text);
        if (vector != null) {
            return vector;
        }
        vector = model.encode(text);
        cacheManager.put(text, vector);
        return vector;
    }
    
    @Override
    @Log(module = "向量化", operation = "批量向量化", description = "批量文本向量化")
    public List<float[]> batchVectorize(List<String> texts) {
        return model.encode(texts).stream()
            .map(this::normalize)
            .collect(Collectors.toList());
    }
    
    @Override
    @Log(module = "向量化", operation = "缓存向量化", description = "带缓存的文本向量化")
    @Cache(name = "vector", key = "#text", expire = 3600)
    public float[] vectorizeWithCache(String text) {
        String cacheKey = String.valueOf(text.hashCode());
        float[] cachedVector = cacheManager.getVector(cacheKey);
        if (cachedVector != null) {
            return cachedVector;
        }
        float[] vector = vectorize(text);
        cacheManager.put(cacheKey, vector);
        return vector;
    }
    
    @Override
    @Log(module = "向量化", operation = "批量缓存向量化", description = "带缓存的批量文本向量化")
    public List<float[]> batchVectorizeWithCache(List<String> texts) {
        Map<String, String> textToKey = texts.stream()
            .collect(Collectors.toMap(
                text -> text,
                text -> String.valueOf(text.hashCode())
            ));
        Map<String, float[]> cachedVectors = cacheManager.batchGet(
            new ArrayList<>(textToKey.values())
        );
        List<String> textsToVectorize = new ArrayList<>();
        List<float[]> result = new ArrayList<>();
        for (String text : texts) {
            String cacheKey = textToKey.get(text);
            float[] cachedVector = cachedVectors.get(cacheKey);
            if (cachedVector != null) {
                result.add(cachedVector);
            } else {
                textsToVectorize.add(text);
                result.add(null);
            }
        }
        if (!textsToVectorize.isEmpty()) {
            List<float[]> newVectors = batchVectorize(textsToVectorize);
            Map<String, float[]> newCacheEntries = new HashMap<>();
            int newVectorIndex = 0;
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i) == null) {
                    float[] vector = newVectors.get(newVectorIndex++);
                    result.set(i, vector);
                    newCacheEntries.put(textToKey.get(texts.get(i)), vector);
                }
            }
            cacheManager.batchPut(newCacheEntries);
        }
        return result;
    }
    
    @Override
    @Log(module = "向量化", operation = "获取模型信息", description = "获取向量化模型信息")
    public Map<String, Object> getModelInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("modelName", model.getModelName());
        info.put("modelVersion", model.getModelVersion());
        info.put("vectorDimension", model.getDimension());
        info.put("batchSize", config.getModel().getBatchSize());
        
        // 添加缓存统计信息
        info.putAll(cacheManager.getStats());
        
        return info;
    }
    
    @Override
    @Log(module = "向量化", operation = "更新模型", description = "更新向量化模型")
    public void updateModel(String modelPath) {
        try {
            model.update(modelPath);
        } catch (Exception e) {
            log.error("更新向量化模型失败", e);
            throw new BusinessException("更新向量化模型失败: " + e.getMessage());
        }
    }
    
    @Override
    @Log(module = "向量化", operation = "预热模型", description = "预热向量化模型")
    public void warmupModel() {
        try {
            model.warmup();
        } catch (Exception e) {
            log.error("向量化模型预热失败", e);
            throw new BusinessException("向量化模型预热失败: " + e.getMessage());
        }
    }

    public float[] normalize(float[] vector) {
        if (vector == null || vector.length == 0) {
            return vector;
        }
        float sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        float norm = (float) Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    @Override
    public int getDimension() {
        return model.getDimension();
    }

    public CompletableFuture<List<float[]>> batchVectorizeAsync(List<String> texts) {
        return CompletableFuture.supplyAsync(() -> batchVectorize(texts), vectorizationExecutor);
    }

    public float calculateSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            throw new IllegalArgumentException("向量维度不一致或为空");
        }
        float dot = 0, norm1 = 0, norm2 = 0;
        for (int i = 0; i < vector1.length; i++) {
            dot += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        return (float) (dot / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    @Override
    public List<List<Float>> calculateSimilarityMatrix(List<float[]> vectors) {
        int n = vectors.size();
        List<List<Float>> matrix = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            List<Float> row = new ArrayList<>(n);
            for (int j = 0; j < n; j++) {
                row.add(calculateSimilarity(vectors.get(i), vectors.get(j)));
            }
            matrix.add(row);
        }
        return matrix;
    }
} 