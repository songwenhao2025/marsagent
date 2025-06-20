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
    public void storeVector(String id, float[] vector) {
        vectorStore.put(id, vector);
    }

    @Override
    public void storeVectors(Map<String, float[]> vectors) {
        vectorStore.putAll(vectors);
    }

    @Override
    public float[] getVector(String id) {
        return vectorStore.get(id);
    }

    @Override
    public Map<String, float[]> getVectors(List<String> ids) {
        Map<String, float[]> result = new HashMap<>();
        for (String id : ids) {
            float[] v = vectorStore.get(id);
            if (v != null) result.put(id, v);
        }
        return result;
    }

    @Override
    public void deleteVector(String id) {
        vectorStore.remove(id);
    }

    @Override
    public void deleteVectors(List<String> ids) {
        ids.forEach(vectorStore::remove);
    }

    @Override
    public Map<String, Float> searchSimilar(float[] queryVector, int limit, float minScore) {
        List<Map.Entry<String, Float>> results = searchInternal(queryVector, null, limit, minScore);
        return results.stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                LinkedHashMap::new
            ));
    }

    @Override
    public Map<String, Float> searchSimilarInRange(float[] queryVector, List<String> vectorIds, int limit, float minScore) {
        List<Map.Entry<String, Float>> results = vectorIds.stream()
            .filter(vectorStore::containsKey)
            .map(id -> new AbstractMap.SimpleEntry<>(id, cosineSimilarity(queryVector, vectorStore.get(id))))
            .filter(entry -> entry.getValue() >= minScore)
            .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());

        return results.stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                LinkedHashMap::new
            ));
    }

    private List<Map.Entry<String, Float>> searchInternal(float[] queryVector, String prefix, int limit, float minScore) {
        // 计算相似度并排序
        return vectorStore.entrySet().stream()
            .filter(entry -> prefix == null || entry.getKey().startsWith(prefix))
            .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), cosineSimilarity(queryVector, entry.getValue())))
            .filter(entry -> entry.getValue() >= minScore)
            .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

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