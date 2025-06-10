package com.marsreg.document.config;

import com.marsreg.vector.service.VectorizationService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public VectorizationService mockVectorizationService() {
        return new VectorizationService() {
            private final Random random = new Random();
            private static final int DIMENSION = 384;

            @Override
            public float[] vectorize(String text) {
                return generateRandomVector(DIMENSION);
            }

            @Override
            public List<float[]> batchVectorize(List<String> texts) {
                return texts.stream()
                        .map(text -> generateRandomVector(DIMENSION))
                        .collect(Collectors.toList());
            }

            @Override
            public float[] vectorizeWithCache(String text) {
                return vectorize(text);
            }

            @Override
            public List<float[]> batchVectorizeWithCache(List<String> texts) {
                return batchVectorize(texts);
            }

            @Override
            public Map<String, Object> getModelInfo() {
                Map<String, Object> info = new HashMap<>();
                info.put("modelName", "mock-model");
                info.put("modelVersion", "1.0.0");
                info.put("vectorDimension", DIMENSION);
                return info;
            }

            @Override
            public void updateModel(String modelPath) {
                // 模拟更新模型
            }

            @Override
            public void warmupModel() {
                // 模拟预热模型
            }

            @Override
            public float calculateSimilarity(float[] vector1, float[] vector2) {
                return calculateCosineSimilarity(vector1, vector2);
            }

            @Override
            public List<List<Float>> calculateSimilarityMatrix(List<float[]> vectors) {
                int n = vectors.size();
                List<List<Float>> matrix = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    List<Float> row = new ArrayList<>(n);
                    for (int j = 0; j < n; j++) {
                        row.add(calculateCosineSimilarity(vectors.get(i), vectors.get(j)));
                    }
                    matrix.add(row);
                }
                return matrix;
            }

            @Override
            public int getDimension() {
                return DIMENSION;
            }

            @Override
            public float[] normalize(float[] vector) {
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

            private float[] generateRandomVector(int dimension) {
                float[] vector = new float[dimension];
                for (int i = 0; i < dimension; i++) {
                    vector[i] = random.nextFloat() * 2 - 1;
                }
                return normalize(vector);
            }

            private float calculateCosineSimilarity(float[] vector1, float[] vector2) {
                float dotProduct = 0;
                float norm1 = 0;
                float norm2 = 0;
                for (int i = 0; i < vector1.length; i++) {
                    dotProduct += vector1[i] * vector2[i];
                    norm1 += vector1[i] * vector1[i];
                    norm2 += vector2[i] * vector2[i];
                }
                return dotProduct / ((float) Math.sqrt(norm1) * (float) Math.sqrt(norm2));
            }
        };
    }
} 