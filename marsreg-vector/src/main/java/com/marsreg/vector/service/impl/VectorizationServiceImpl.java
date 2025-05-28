package com.marsreg.vector.service.impl;

import com.marsreg.vector.service.VectorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class VectorizationServiceImpl implements VectorizationService {
    private static final int VECTOR_DIMENSION = 1536; // OpenAI text-embedding-ada-002 模型的维度
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public float[] vectorize(String text) {
        try {
            // 这里使用随机向量作为示例，实际应用中应该使用真实的向量化模型
            float[] vector = new float[VECTOR_DIMENSION];
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                vector[i] = (float) (Math.random() * 2 - 1);
            }
            return normalize(vector);
        } catch (Exception e) {
            log.error("向量化文本失败: {}", text, e);
            throw new RuntimeException("向量化文本失败", e);
        }
    }

    @Override
    public List<float[]> batchVectorize(List<String> texts) {
        List<CompletableFuture<float[]>> futures = new ArrayList<>();
        for (String text : texts) {
            CompletableFuture<float[]> future = CompletableFuture.supplyAsync(() -> vectorize(text), executorService);
            futures.add(future);
        }

        List<float[]> results = new ArrayList<>();
        for (CompletableFuture<float[]> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                log.error("批量向量化失败", e);
                throw new RuntimeException("批量向量化失败", e);
            }
        }
        return results;
    }

    @Override
    public int getDimension() {
        return VECTOR_DIMENSION;
    }

    @Override
    public float calculateSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }
        float dotProduct = 0;
        float norm1 = 0;
        float norm2 = 0;
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        return dotProduct / (float) Math.sqrt(norm1 * norm2);
    }

    public float[] normalize(float[] vector) {
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        float[] normalized = new float[vector.length];
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                normalized[i] = vector[i] / norm;
            }
        }
        return normalized;
    }
} 