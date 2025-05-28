package com.marsreg.vector.service.impl;

import com.marsreg.vector.VectorApplication;
import com.marsreg.vector.model.SearchResult;
import com.marsreg.vector.service.impl.HuggingFaceVectorizationService;
import io.milvus.client.MilvusClient;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = VectorApplication.class)
@ActiveProfiles("test")
public class MilvusVectorStorageServiceTest {

    @MockBean
    private MilvusClient milvusClient;

    @MockBean
    private HuggingFaceVectorizationService huggingFaceVectorizationService;

    @Autowired
    private MilvusVectorStorageService vectorStorageService;

    private static final String COLLECTION_NAME = "test_collection";
    private static final int DIMENSION = 1536;
    private static final String INDEX_TYPE = "IVF_FLAT";
    private static final int NLIST = 1024;
    private static final String METRIC_TYPE = "COSINE";

    @BeforeEach
    void setUp() {
        // 初始化服务
        vectorStorageService.init();
        // 清空所有 test_ 开头的数据，保证测试隔离
        vectorStorageService.deleteByPrefix("test_");
    }

    @Test
    void search_ShouldReturnSearchResults() {
        // mock normalize 方法，返回原始向量，避免 NPE
        when(huggingFaceVectorizationService.normalize(any(float[].class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // 准备测试数据
        float[] vector1 = new float[DIMENSION];
        float[] vector2 = new float[DIMENSION];
        Arrays.fill(vector1, 0.1f);
        Arrays.fill(vector2, 0.2f);

        // 归一化向量
        float[] normalizedVector1 = huggingFaceVectorizationService.normalize(vector1);
        float[] normalizedVector2 = huggingFaceVectorizationService.normalize(vector2);

        // 插入测试数据
        vectorStorageService.store("test_1", normalizedVector1);
        vectorStorageService.store("test_2", normalizedVector2);

        // 执行搜索
        List<Map.Entry<String, Float>> results = vectorStorageService.search(normalizedVector1, 5, 0.0f);

        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        
        // 验证结果顺序和相似度
        boolean found = false;
        for (Map.Entry<String, Float> entry : results) {
            if ("test_1".equals(entry.getKey())) {
                assertTrue(entry.getValue() > 0.9f, "test_1 的相似度应该大于0.9");
                found = true;
            }
        }
        assertTrue(found, "结果中应包含 test_1");
    }

    @Test
    void searchByPrefix_ShouldReturnSearchResults() {
        // 准备测试数据
        float[] queryVector = new float[DIMENSION];
        Arrays.fill(queryVector, 0.1f);
        String prefix = "test_";
        int limit = 5;
        float minScore = 0.5f;

        // 执行搜索
        List<Map.Entry<String, Float>> results = vectorStorageService.searchByPrefix(queryVector, prefix, limit, minScore);

        // 验证结果
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void store_ShouldStoreVector() {
        // 准备测试数据
        String id = "test_1";
        float[] vector = new float[DIMENSION];
        Arrays.fill(vector, 0.1f);

        // 执行存储
        vectorStorageService.store(id, vector);

        // 验证结果
        List<Map.Entry<String, Float>> results = vectorStorageService.search(vector, 1, 0.5f);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(id, results.get(0).getKey());
    }

    @Test
    void delete_ShouldDeleteVector() {
        // 准备测试数据
        String id = "test_1";
        float[] vector = new float[DIMENSION];
        Arrays.fill(vector, 0.1f);

        // 存储向量
        vectorStorageService.store(id, vector);

        // 执行删除
        vectorStorageService.delete(id);

        // 验证结果
        List<Map.Entry<String, Float>> results = vectorStorageService.search(vector, 1, 0.5f);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void deleteByPrefix_ShouldDeleteVectors() {
        // 准备测试数据
        String prefix = "test_";
        String id1 = prefix + "1";
        String id2 = prefix + "2";
        float[] vector = new float[DIMENSION];
        Arrays.fill(vector, 0.1f);

        // 存储向量
        vectorStorageService.store(id1, vector);
        vectorStorageService.store(id2, vector);

        // 执行删除
        vectorStorageService.deleteByPrefix(prefix);

        // 验证结果
        List<Map.Entry<String, Float>> results = vectorStorageService.search(vector, 2, 0.5f);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
} 