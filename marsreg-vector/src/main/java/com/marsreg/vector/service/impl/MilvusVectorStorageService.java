package com.marsreg.vector.service.impl;

import com.marsreg.vector.model.SearchResult;
import com.marsreg.vector.service.VectorStorageService;
import io.milvus.client.MilvusClient;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.milvus.param.MetricType;
import io.milvus.param.IndexType;
import io.milvus.param.collection.FieldType;
import io.milvus.grpc.DataType;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorStorageService implements VectorStorageService {

    private final MilvusClient milvusClient;

    @Value("${marsreg.vector.milvus.collection:marsreg_vectors}")
    private String collectionName;

    @Value("${marsreg.vector.milvus.dimension:384}")
    private int dimension;

    @Value("${marsreg.vector.milvus.index.type:IVF_SQ8}")
    private String indexType;

    @Value("${marsreg.vector.milvus.index.nlist:1024}")
    private int nlist;

    @Value("${marsreg.vector.milvus.index.metric-type:COSINE}")
    private String metricType;

    private final Map<String, float[]> vectorStore = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            // 检查集合是否存在
            R<Boolean> hasCollection = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());

            if (hasCollection == null || !hasCollection.getData()) {
                // 创建集合
                CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withDescription("Vector collection for MarsReg")
                        .withShardsNum(2)
                        .addFieldType(FieldType.newBuilder()
                                .withName("id")
                                .withDataType(DataType.VarChar)
                                .withMaxLength(100)
                                .withPrimaryKey(true)
                                .withAutoID(false)
                                .build())
                        .addFieldType(FieldType.newBuilder()
                                .withName("vector")
                                .withDataType(DataType.FloatVector)
                                .withDimension(dimension)
                                .build())
                        .build();
                milvusClient.createCollection(createCollectionParam);

                // 创建索引
                CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withFieldName("vector")
                        .withIndexType(IndexType.valueOf(indexType))
                        .withMetricType(MetricType.valueOf(metricType))
                        .withExtraParam("{\"nlist\":" + nlist + "}")
                        .build();
                milvusClient.createIndex(createIndexParam);
            }

            log.info("Milvus集合初始化成功: {}", collectionName);
        } catch (Exception e) {
            log.error("Milvus集合初始化失败", e);
            throw new RuntimeException("Milvus集合初始化失败", e);
        }
    }

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
        if (vectorStore.containsKey(id)) {
            vectorStore.put(id, vector);
        } else {
            throw new IllegalArgumentException("向量不存在: " + id);
        }
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
        List<Map.Entry<String, Float>> results = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : vectorStore.entrySet()) {
            float score = calculateCosineSimilarity(queryVector, entry.getValue());
            if (score >= minScore) {
                results.add(Map.entry(entry.getKey(), score));
            }
        }
        results.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        return results.subList(0, Math.min(limit, results.size()));
    }

    @Override
    public List<Map.Entry<String, Float>> searchByPrefix(float[] queryVector, String prefix, int limit, float minScore) {
        List<Map.Entry<String, Float>> results = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : vectorStore.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                float score = calculateCosineSimilarity(queryVector, entry.getValue());
                if (score >= minScore) {
                    results.add(Map.entry(entry.getKey(), score));
                }
            }
        }
        results.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        return results.subList(0, Math.min(limit, results.size()));
    }

    private float calculateCosineSimilarity(float[] vector1, float[] vector2) {
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
} 