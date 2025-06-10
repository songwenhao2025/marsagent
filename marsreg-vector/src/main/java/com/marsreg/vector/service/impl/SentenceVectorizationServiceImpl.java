package com.marsreg.vector.service.impl;

import com.marsreg.vector.model.SentenceTransformerModel;
import com.marsreg.vector.service.SentenceVectorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

@Service
public class SentenceVectorizationServiceImpl implements SentenceVectorizationService {
    private final SentenceTransformerModel model;
    private final ExecutorService vectorizationExecutor;

    @Autowired
    public SentenceVectorizationServiceImpl(SentenceTransformerModel model) {
        this.model = model;
        this.vectorizationExecutor = Executors.newFixedThreadPool(4);
    }

    @Override
    public int getDimension() {
        return model.getDimension();
    }

    @Override
    public float[] vectorize(String text) {
        return model.encode(text);
    }

    @Override
    public List<float[]> batchVectorize(List<String> texts) {
        return model.encode(texts);
    }

    @Override
    public CompletableFuture<List<float[]>> batchVectorizeAsync(List<String> texts) {
        return CompletableFuture.supplyAsync(() -> batchVectorize(texts), vectorizationExecutor);
    }

    @Override
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
    public float calculateSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) {
            throw new IllegalArgumentException("向量维度不一致或为空");
        }
        float dot = 0, norm1 = 0, norm2 = 0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        return (float) (dot / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    @Override
    public void warmupModel() {
        // 简单实现：调用一次 encode 进行预热
        model.encode(List.of("预热向量化模型"));
    }

    @Override
    public void updateModel(String modelPath) {
        throw new UnsupportedOperationException("暂不支持更新模型");
    }

    @Override
    public Map<String, Object> getModelInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("modelName", model.getModelName());
        info.put("modelVersion", model.getModelVersion());
        info.put("vectorDimension", model.getDimension());
        return info;
    }

    @Override
    public List<float[]> batchVectorizeWithCache(List<String> texts) {
        // 由于 SentenceVectorizationService 不涉及缓存，直接调用 batchVectorize
        return batchVectorize(texts);
    }

    @Override
    public float[] vectorizeWithCache(String text) {
        // 由于 SentenceVectorizationService 不涉及缓存，直接调用 vectorize
        return vectorize(text);
    }

    @Override
    public List<List<Float>> calculateSimilarityMatrix(List<float[]> vectors) {
        int n = vectors.size();
        List<List<Float>> matrix = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Float> row = new java.util.ArrayList<>();
            for (int j = 0; j < n; j++) {
                row.add(calculateSimilarity(vectors.get(i), vectors.get(j)));
            }
            matrix.add(row);
        }
        return matrix;
    }
} 