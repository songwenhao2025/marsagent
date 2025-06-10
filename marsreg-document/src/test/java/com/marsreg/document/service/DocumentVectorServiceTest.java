package com.marsreg.document.service;

import com.marsreg.document.config.TestConfig;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.entity.DocumentChunk;
import com.marsreg.document.repository.DocumentChunkRepository;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
public class DocumentVectorServiceTest {

    @Autowired
    private DocumentVectorService documentVectorService;

    @MockBean
    private VectorizationService vectorizationService;

    @MockBean
    private VectorStorageService vectorStorageService;

    @MockBean
    private DocumentChunkRepository documentChunkRepository;

    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

    private DocumentEntity testDocument;
    private List<DocumentChunk> testChunks;

    @BeforeEach
    void setUp() {
        // 创建测试文档
        testDocument = new DocumentEntity();
        testDocument.setId(1L);
        testDocument.setName("test.pdf");
        testDocument.setOriginalName("test.pdf");
        testDocument.setContentType("application/pdf");
        testDocument.setContent("这是测试文档的内容");

        // 创建测试分块
        DocumentChunk chunk1 = new DocumentChunk();
        chunk1.setId(1L);
        chunk1.setDocument(testDocument);
        chunk1.setContent("这是第一个分块的内容");
        chunk1.setChunkIndex(0);

        DocumentChunk chunk2 = new DocumentChunk();
        chunk2.setId(2L);
        chunk2.setDocument(testDocument);
        chunk2.setContent("这是第二个分块的内容");
        chunk2.setChunkIndex(1);

        testChunks = Arrays.asList(chunk1, chunk2);

        // 模拟向量化服务的行为
        when(vectorizationService.vectorize(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        when(vectorizationService.calculateSimilarity(any(float[].class), any(float[].class))).thenReturn(0.8f);
    }

    @Test
    void testGenerateVector() {
        // 执行测试
        float[] vector = documentVectorService.generateVector(testDocument);

        // 验证结果
        assertNotNull(vector);
        assertEquals(3, vector.length);
        verify(vectorizationService, times(1)).vectorize(testDocument.getContent());
    }

    @Test
    void testGenerateVectors() {
        // 准备测试数据
        List<DocumentEntity> documents = Arrays.asList(testDocument);

        // 执行测试
        Map<Long, float[]> vectors = documentVectorService.generateVectors(documents);

        // 验证结果
        assertNotNull(vectors);
        assertEquals(1, vectors.size());
        assertTrue(vectors.containsKey(testDocument.getId()));
        verify(vectorizationService, times(1)).vectorize(testDocument.getContent());
    }

    @Test
    void testCalculateSimilarity() {
        // 准备测试数据
        float[] vector1 = new float[]{0.1f, 0.2f, 0.3f};
        float[] vector2 = new float[]{0.4f, 0.5f, 0.6f};

        // 执行测试
        float similarity = documentVectorService.calculateSimilarity(vector1, vector2);

        // 验证结果
        assertEquals(0.8f, similarity);
        verify(vectorizationService, times(1)).calculateSimilarity(vector1, vector2);
    }

    @Test
    void testVectorizeAndStore() {
        // 执行测试
        documentVectorService.vectorizeAndStore(testDocument, testChunks);

        // 验证VectorStorageService的调用
        verify(vectorStorageService, times(2)).storeVector(anyString(), any(float[].class));
    }

    @Test
    void testBatchVectorizeAndStore() {
        // 准备测试数据
        Map<Long, List<DocumentChunk>> documentChunksMap = Map.of(
            testDocument.getId(), testChunks
        );

        // 执行测试
        documentVectorService.batchVectorizeAndStore(documentChunksMap);

        // 验证VectorStorageService的调用
        verify(vectorStorageService, times(2)).storeVector(anyString(), any(float[].class));
    }

    @Test
    void testDeleteDocumentVectors() {
        // 模拟文档分块查询
        when(documentChunkRepository.findByDocumentId(testDocument.getId())).thenReturn(testChunks);

        // 执行测试
        documentVectorService.deleteDocumentVectors(testDocument.getId());

        // 验证VectorStorageService的调用
        verify(vectorStorageService, times(2)).deleteVector(anyString());
    }

    @Test
    void testUpdateChunkVector() {
        // 准备测试数据
        Long documentId = testDocument.getId();
        Long chunkId = testChunks.get(0).getId();
        String content = "更新后的内容";

        // 执行测试
        documentVectorService.updateChunkVector(documentId, chunkId, content);

        // 验证VectorStorageService的调用
        verify(vectorStorageService, times(1)).storeVector(anyString(), any(float[].class));
    }

    @Test
    void testSearchChunks() {
        // 准备测试数据
        String query = "测试查询";
        int limit = 10;
        float minScore = 0.5f;

        // 模拟搜索结果
        when(vectorStorageService.searchSimilar(any(float[].class), eq(limit), eq(minScore)))
            .thenReturn(Map.of(
                "1_1", 0.8f,
                "1_2", 0.7f
            ));

        // 执行测试
        List<Map<String, Object>> results = documentVectorService.searchChunks(query, limit, minScore);

        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        verify(vectorStorageService, times(1)).searchSimilar(any(float[].class), eq(limit), eq(minScore));
    }

    @Test
    void testSearchChunksByDocument() {
        // 准备测试数据
        Long documentId = testDocument.getId();
        String query = "测试查询";
        int limit = 10;
        float minScore = 0.5f;

        // 模拟文档分块查询
        when(documentChunkRepository.findByDocumentId(documentId)).thenReturn(testChunks);

        // 模拟搜索结果
        when(vectorStorageService.searchSimilarInRange(any(float[].class), any(List.class), eq(limit), eq(minScore)))
            .thenReturn(Map.of(
                "1_1", 0.8f,
                "1_2", 0.7f
            ));

        // 执行测试
        List<Map<String, Object>> results = documentVectorService.searchChunksByDocument(documentId, query, limit, minScore);

        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        verify(vectorStorageService, times(1)).searchSimilarInRange(any(float[].class), any(List.class), eq(limit), eq(minScore));
    }
} 