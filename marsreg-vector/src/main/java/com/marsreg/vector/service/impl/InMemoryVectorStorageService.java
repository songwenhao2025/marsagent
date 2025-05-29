package com.marsreg.vector.service.impl;

import com.marsreg.vector.service.VectorStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@Profile("test")
public class InMemoryVectorStorageService implements VectorStorageService {

    // 使用内存存储作为测试环境实现
    private final Map<String, float[]> vectorStore = new ConcurrentHashMap<>();

    @Override
    public void store(String id, float[] vector) {
        vectorStore.put(id, vector);
    }

    @Override
    public void batchStore(Map<String, float[]> vectors) {
        vectorStore.putAll(vectors);
    }

    @Override
    public void updateVector(String id, float[] vector) {
        vectorStore.put(id, vector);
    }

    @Override
    public void delete(String id) {
        vectorStore.remove(id);
    }

    @Override
    public void deleteByPrefix(String prefix) {
        vectorStore.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Override
    public List<Map.Entry<String, Float>> search(float[] queryVector, int limit, float minScore) {
        return searchInternal(queryVector, null, limit, minScore);
    }

    @Override
    public List<Map.Entry<String, Float>> searchByPrefix(float[] queryVector, String prefix, int limit, float minScore) {
        return searchInternal(queryVector, prefix, limit, minScore);
    }

    private List<Map.Entry<String, Float>> searchInternal(float[] queryVector, String prefix, int limit, float minScore) {
        // 计算相似度并排序
        List<Map.Entry<String, Float>> results = vectorStore.entrySet().stream()
                .filter(entry -> prefix == null || entry.getKey().startsWith(prefix))
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), cosineSimilarity(queryVector, entry.getValue())))
                .filter(entry -> entry.getValue() >= minScore)
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        return results;
    }

    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(float[] vector1, float[] vector2) {
        float dotProduct = 0;
        float norm1 = 0;
        float norm2 = 0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
} 