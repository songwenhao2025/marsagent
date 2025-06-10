package com.marsreg.vector.service.impl;

import com.marsreg.vector.service.VectorStorageService;
import io.milvus.client.MilvusClient;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.DeleteParam;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    @Value("${marsreg.vector.milvus.index.metric-type:IP}")
    private String metricType;

    private final Map<String, float[]> vectorStore = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            // 检查集合是否存在
            R<Boolean> hasCollection = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());

            if (hasCollection == null || hasCollection.getData() == null || !hasCollection.getData()) {
                // 创建集合
                CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withDescription("向量存储集合")
                        .withShardsNum(2)
                        .addFieldType(FieldType.newBuilder()
                                .withName("id")
                                .withDataType(DataType.VarChar)
                                .withMaxLength(64)
                                .withPrimaryKey(true)
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
                log.info("Milvus集合和索引创建成功");
            }
        } catch (Exception e) {
            log.error("Milvus初始化失败", e);
            throw new RuntimeException("Milvus初始化失败", e);
        }
    }

    @Override
    public void storeVector(String id, float[] vector) {
        try {
            List<String> ids = Collections.singletonList(id);
            List<List<Float>> vectors = Collections.singletonList(
                IntStream.range(0, vector.length)
                    .mapToObj(i -> (float) vector[i])
                    .collect(Collectors.toList())
            );

            List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field("id", ids),
                new InsertParam.Field("vector", vectors)
            );
            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

            milvusClient.insert(insertParam);
            vectorStore.put(id, vector);
        } catch (Exception e) {
            log.error("向量存储失败: " + id, e);
            throw new RuntimeException("向量存储失败", e);
        }
    }

    @Override
    public void storeVectors(Map<String, float[]> vectors) {
        try {
            List<String> ids = new ArrayList<>(vectors.keySet());
            List<List<Float>> vectorList = vectors.values().stream()
                    .map(vector -> IntStream.range(0, vector.length)
                            .mapToObj(i -> (float) vector[i])
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());

            List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field("id", ids),
                new InsertParam.Field("vector", vectorList)
            );
            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

            milvusClient.insert(insertParam);
            vectorStore.putAll(vectors);
        } catch (Exception e) {
            log.error("批量向量存储失败", e);
            throw new RuntimeException("批量向量存储失败", e);
        }
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
        try {
            milvusClient.delete(DeleteParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr("id == \"" + id + "\"")
                    .build());
            vectorStore.remove(id);
        } catch (Exception e) {
            log.error("向量删除失败: " + id, e);
            throw new RuntimeException("向量删除失败", e);
        }
    }

    @Override
    public void deleteVectors(List<String> ids) {
        try {
            String expr = ids.stream()
                    .map(id -> "id == \"" + id + "\"")
                    .collect(Collectors.joining(" || "));

            milvusClient.delete(DeleteParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr(expr)
                    .build());

            ids.forEach(vectorStore::remove);
        } catch (Exception e) {
            log.error("批量向量删除失败", e);
            throw new RuntimeException("批量向量删除失败", e);
        }
    }

    @Override
    public Map<String, Float> searchSimilar(float[] queryVector, int limit, float minScore) {
        try {
            List<List<Float>> vectors = Collections.singletonList(
                IntStream.range(0, queryVector.length)
                    .mapToObj(i -> (float) queryVector[i])
                    .collect(Collectors.toList())
            );

            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withVectorFieldName("vector")
                    .withVectors(vectors)
                    .withTopK(limit)
                    .withMetricType(MetricType.valueOf(metricType))
                    .withParams("{\"nprobe\":10}")
                    .build();

            R<SearchResults> response = milvusClient.search(searchParam);
            SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());

            Map<String, Float> results = new LinkedHashMap<>();
            List<?> idList = wrapper.getFieldData("id", 0);
            List<SearchResultsWrapper.IDScore> idScoreList = wrapper.getIDScore(0);
            for (int i = 0; i < idScoreList.size(); i++) {
                float score = idScoreList.get(i).getScore();
                if (score >= minScore) {
                    results.put(String.valueOf(idList.get(i)), score);
                }
            }
            return results;
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            throw new RuntimeException("向量搜索失败", e);
        }
    }

    @Override
    public Map<String, Float> searchSimilarInRange(float[] queryVector, List<String> vectorIds, int limit, float minScore) {
        try {
            List<List<Float>> vectors = Collections.singletonList(
                IntStream.range(0, queryVector.length)
                    .mapToObj(i -> (float) queryVector[i])
                    .collect(Collectors.toList())
            );

            String expr = vectorIds.stream()
                    .map(id -> "id == \"" + id + "\"")
                    .collect(Collectors.joining(" || "));

            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withVectorFieldName("vector")
                    .withVectors(vectors)
                    .withTopK(limit)
                    .withMetricType(MetricType.valueOf(metricType))
                    .withParams("{\"nprobe\":10}")
                    .withExpr(expr)
                    .build();

            R<SearchResults> response = milvusClient.search(searchParam);
            SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());

            Map<String, Float> results = new LinkedHashMap<>();
            List<?> idList = wrapper.getFieldData("id", 0);
            List<SearchResultsWrapper.IDScore> idScoreList = wrapper.getIDScore(0);
            for (int i = 0; i < idScoreList.size(); i++) {
                float score = idScoreList.get(i).getScore();
                if (score >= minScore) {
                    results.put(String.valueOf(idList.get(i)), score);
                }
            }
            return results;
        } catch (Exception e) {
            log.error("范围向量搜索失败", e);
            throw new RuntimeException("范围向量搜索失败", e);
        }
    }
} 