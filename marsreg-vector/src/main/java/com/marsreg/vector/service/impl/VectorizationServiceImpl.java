package com.marsreg.vector.service.impl;

import com.marsreg.common.annotation.Cache;
import com.marsreg.common.annotation.Log;
import com.marsreg.common.exception.BusinessException;
import com.marsreg.vector.cache.VectorCacheManager;
import com.marsreg.vector.config.VectorizationConfig;
import com.marsreg.vector.model.VectorizationModel;
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
    private final VectorizationModel model;
    private final VectorCacheManager cacheManager;
    
    private static final int BATCH_SIZE = 32; // 批处理大小
    
    @Override
    @Log(module = "向量化", operation = "向量化", description = "文本向量化")
    public List<Float> vectorize(String text) {
        try {
            return model.vectorize(text);
        } catch (Exception e) {
            log.error("文本向量化失败", e);
            throw new BusinessException("文本向量化失败: " + e.getMessage());
        }
    }
    
    @Override
    @Log(module = "向量化", operation = "批量向量化", description = "批量文本向量化")
    public List<List<Float>> batchVectorize(List<String> texts) {
        try {
            // 如果文本数量小于批处理大小，直接处理
            if (texts.size() <= BATCH_SIZE) {
                return model.batchVectorize(texts);
            }
            
            // 将文本列表分成多个批次
            List<List<String>> batches = new ArrayList<>();
            for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
                batches.add(texts.subList(i, Math.min(i + BATCH_SIZE, texts.size())));
            }
            
            // 并行处理每个批次
            List<CompletableFuture<List<List<Float>>>> futures = batches.stream()
                .map(batch -> CompletableFuture.supplyAsync(() -> model.batchVectorize(batch), vectorizationExecutor))
                .collect(Collectors.toList());
            
            // 等待所有批次处理完成并合并结果
            return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("批量文本向量化失败", e);
            throw new BusinessException("批量文本向量化失败: " + e.getMessage());
        }
    }
    
    @Override
    @Log(module = "向量化", operation = "缓存向量化", description = "带缓存的文本向量化")
    @Cache(name = "vector", key = "#text", expire = 3600)
    public List<Float> vectorizeWithCache(String text) {
        String cacheKey = String.valueOf(text.hashCode());
        
        // 尝试从缓存获取
        List<Float> cachedVector = cacheManager.get(cacheKey);
        if (cachedVector != null) {
            return cachedVector;
        }
        
        // 缓存未命中，进行向量化
        List<Float> vector = vectorize(text);
        
        // 存入缓存
        cacheManager.put(cacheKey, vector);
        
        return vector;
    }
    
    @Override
    @Log(module = "向量化", operation = "批量缓存向量化", description = "带缓存的批量文本向量化")
    public List<List<Float>> batchVectorizeWithCache(List<String> texts) {
        // 生成缓存键
        Map<String, String> textToKey = texts.stream()
            .collect(Collectors.toMap(
                text -> text,
                text -> String.valueOf(text.hashCode())
            ));
        
        // 批量获取缓存
        Map<String, List<Float>> cachedVectors = cacheManager.batchGet(
            textToKey.values().stream().collect(Collectors.toList())
        );
        
        // 找出缓存未命中的文本
        List<String> textsToVectorize = new ArrayList<>();
        List<List<Float>> result = new ArrayList<>();
        
        for (String text : texts) {
            String cacheKey = textToKey.get(text);
            List<Float> cachedVector = cachedVectors.get(cacheKey);
            if (cachedVector != null) {
                result.add(cachedVector);
            } else {
                textsToVectorize.add(text);
                result.add(null);
            }
        }
        
        // 对未命中的文本进行向量化
        if (!textsToVectorize.isEmpty()) {
            List<List<Float>> newVectors = batchVectorize(textsToVectorize);
            
            // 更新缓存
            Map<String, List<Float>> newCacheEntries = new HashMap<>();
            int newVectorIndex = 0;
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i) == null) {
                    List<Float> vector = newVectors.get(newVectorIndex++);
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
} 