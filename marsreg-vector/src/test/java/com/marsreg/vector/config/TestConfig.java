package com.marsreg.vector.config;

import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import com.marsreg.vector.service.impl.MilvusVectorStorageService;
import io.milvus.client.MilvusClient;
import io.milvus.param.R;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.grpc.SearchResults;
import io.milvus.param.dml.SearchParam;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            public int getDimension() {
                return DIMENSION;
            }

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
            public float calculateSimilarity(float[] vector1, float[] vector2) {
                float dotProduct = 0.0f;
                float norm1 = 0.0f;
                float norm2 = 0.0f;

                for (int i = 0; i < vector1.length; i++) {
                    dotProduct += vector1[i] * vector2[i];
                    norm1 += vector1[i] * vector1[i];
                    norm2 += vector2[i] * vector2[i];
                }

                return dotProduct / (float) Math.sqrt(norm1 * norm2);
            }

            @Override
            public float[] normalize(float[] vector) {
                float norm = 0.0f;
                for (float v : vector) {
                    norm += v * v;
                }
                norm = (float) Math.sqrt(norm);
                
                float[] normalized = new float[vector.length];
                for (int i = 0; i < vector.length; i++) {
                    normalized[i] = vector[i] / norm;
                }
                return normalized;
            }

            private float[] generateRandomVector(int dimension) {
                float[] vector = new float[dimension];
                for (int i = 0; i < dimension; i++) {
                    vector[i] = random.nextFloat() * 2 - 1; // 生成 -1 到 1 之间的随机数
                }
                return normalize(vector);
            }
        };
    }

    @Bean
    @Primary
    public MilvusClient mockMilvusClient() {
        MilvusClient mockClient = mock(MilvusClient.class);
        
        // 模拟 hasCollection 调用
        R<Boolean> hasCollectionResponse = R.success(true);
        when(mockClient.hasCollection(any(HasCollectionParam.class))).thenReturn(hasCollectionResponse);
        
        // 模拟 createCollection 调用
        when(mockClient.createCollection(any(CreateCollectionParam.class))).thenReturn(R.success(null));
        
        // 模拟 createIndex 调用
        when(mockClient.createIndex(any(CreateIndexParam.class))).thenReturn(R.success(null));
        
        // mock search 方法，返回空结果，避免类型报错
        when(mockClient.search(any(SearchParam.class))).thenReturn(R.success(SearchResults.newBuilder().build()));
        
        return mockClient;
    }

    @Bean
    @Primary
    public VectorStorageService mockVectorStorageService(MilvusClient milvusClient) {
        return new MilvusVectorStorageService(milvusClient);
    }
} 