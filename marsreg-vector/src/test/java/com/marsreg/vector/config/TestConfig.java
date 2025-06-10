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
import io.milvus.grpc.SearchResultData;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.DeleteParam;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.djl.Model;

@TestConfiguration
@Profile("test")
@TestPropertySource(properties = {
    "marsreg.vector.milvus.collection=test_collection",
    "marsreg.vector.milvus.dimension=384",
    "marsreg.vector.milvus.index.type=IVF_SQ8",
    "marsreg.vector.milvus.index.nlist=1024",
    "marsreg.vector.milvus.index.metric-type=COSINE"
})
public class TestConfig {

    @Bean
    @Primary
    public VectorizationConfig.Model modelConfig() {
        VectorizationConfig.Model config = new VectorizationConfig.Model();
        config.setName("test-model");
        config.setVersion("1.0.0");
        config.setDimension(384);
        config.setDevice("cpu");
        config.setBatchSize(32);
        return config;
    }

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
                info.put("name", "test-model");
                info.put("version", "1.0.0");
                info.put("dimension", DIMENSION);
                return info;
            }

            @Override
            public void updateModel(String modelPath) {
                // 测试环境不需要实现
            }

            @Override
            public void warmupModel() {
                // 测试环境不需要实现
            }

            @Override
            public float calculateSimilarity(float[] vector1, float[] vector2) {
                return calculateCosineSimilarity(vector1, vector2);
            }

            @Override
            public float[] normalize(float[] vector) {
                float sum = 0;
                for (float v : vector) {
                    sum += v * v;
                }
                float norm = (float) Math.sqrt(sum);
                float[] normalized = new float[vector.length];
                for (int i = 0; i < vector.length; i++) {
                    normalized[i] = vector[i] / norm;
                }
                return normalized;
            }

            @Override
            public List<List<Float>> calculateSimilarityMatrix(List<float[]> vectors) {
                List<List<Float>> matrix = new ArrayList<>();
                for (float[] v1 : vectors) {
                    List<Float> row = new ArrayList<>();
                    for (float[] v2 : vectors) {
                        row.add(calculateCosineSimilarity(v1, v2));
                    }
                    matrix.add(row);
                }
                return matrix;
            }

            private float[] generateRandomVector(int dimension) {
                float[] vector = new float[dimension];
                for (int i = 0; i < dimension; i++) {
                    vector[i] = random.nextFloat() * 2 - 1; // 生成 -1 到 1 之间的随机数
                }
                return normalize(vector);
            }

            private float calculateCosineSimilarity(float[] v1, float[] v2) {
                float dotProduct = 0.0f;
                float norm1 = 0.0f;
                float norm2 = 0.0f;

                for (int i = 0; i < v1.length; i++) {
                    dotProduct += v1[i] * v2[i];
                    norm1 += v1[i] * v1[i];
                    norm2 += v2[i] * v2[i];
                }

                return dotProduct / (float) Math.sqrt(norm1 * norm2);
            }
        };
    }

    @Bean
    @Primary
    public Model mockDjlModel() {
        return mock(Model.class);
    }

    @Bean
    @Primary
    public VectorStorageService mockVectorStorageService() {
        MilvusClient mockClient = mock(MilvusClient.class);
        
        // mock hasCollection 返回 getData() 为 TRUE
        R<Boolean> hasCollectionR = mock(R.class);
        when(hasCollectionR.getData()).thenReturn(Boolean.TRUE);
        when(mockClient.hasCollection(any(HasCollectionParam.class))).thenReturn(hasCollectionR);

        // mock createCollection 返回 getData() 为 null
        @SuppressWarnings("unchecked")
        R createCollectionR = mock(R.class);
        when(createCollectionR.getData()).thenReturn(null);
        when(mockClient.createCollection(any(CreateCollectionParam.class))).thenReturn(createCollectionR);

        // mock createIndex 返回 getData() 为 null
        @SuppressWarnings("unchecked")
        R createIndexR = mock(R.class);
        when(createIndexR.getData()).thenReturn(null);
        when(mockClient.createIndex(any(CreateIndexParam.class))).thenReturn(createIndexR);

        // mock insert 返回 getData() 为 null
        @SuppressWarnings("unchecked")
        R insertR = mock(R.class);
        when(insertR.getData()).thenReturn(null);
        when(mockClient.insert(any(InsertParam.class))).thenReturn(insertR);

        // mock delete 返回 getData() 为 null
        @SuppressWarnings("unchecked")
        R deleteR = mock(R.class);
        when(deleteR.getData()).thenReturn(null);
        when(mockClient.delete(any(DeleteParam.class))).thenReturn(deleteR);

        // mock search 返回模拟的搜索结果
        @SuppressWarnings("unchecked")
        R<SearchResults> searchR = mock(R.class);
        SearchResults mockSearchResults = mock(SearchResults.class);
        SearchResultData mockResultData = mock(SearchResultData.class);
        
        // 设置模拟的搜索结果
        io.milvus.grpc.IDs mockIds = mock(io.milvus.grpc.IDs.class);
        // TODO: 暂时注释掉有问题的 mock 代码，等待后续修复
        // when(mockIds.getIntIdList()).thenReturn(Arrays.asList(1, 2, 3));
        // when(mockResultData.getIds()).thenReturn(mockIds);
        // when(mockResultData.getScores(0)).thenReturn(0.9f);
        // when(mockResultData.getScores(1)).thenReturn(0.8f);
        // when(mockResultData.getScores(2)).thenReturn(0.7f);
        // when(mockSearchResults.getResults()).thenReturn(mockResultData);
        // when(searchR.getData()).thenReturn(mockSearchResults);
        // when(mockClient.search(any(SearchParam.class))).thenReturn(searchR);

        // 用反射设置字段，避免为 null
        MilvusVectorStorageService service = new MilvusVectorStorageService(mockClient);
        try {
            java.lang.reflect.Field collectionNameField = MilvusVectorStorageService.class.getDeclaredField("collectionName");
            collectionNameField.setAccessible(true);
            collectionNameField.set(service, "test_collection");

            java.lang.reflect.Field dimensionField = MilvusVectorStorageService.class.getDeclaredField("dimension");
            dimensionField.setAccessible(true);
            dimensionField.set(service, 384);

            java.lang.reflect.Field indexTypeField = MilvusVectorStorageService.class.getDeclaredField("indexType");
            indexTypeField.setAccessible(true);
            indexTypeField.set(service, "IVF_SQ8");

            java.lang.reflect.Field nlistField = MilvusVectorStorageService.class.getDeclaredField("nlist");
            nlistField.setAccessible(true);
            nlistField.set(service, 1024);

            java.lang.reflect.Field metricTypeField = MilvusVectorStorageService.class.getDeclaredField("metricType");
            metricTypeField.setAccessible(true);
            metricTypeField.set(service, "COSINE");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return service;
    }
} 