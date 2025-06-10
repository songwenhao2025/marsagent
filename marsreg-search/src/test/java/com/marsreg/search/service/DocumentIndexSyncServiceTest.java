package com.marsreg.search.service;

import com.marsreg.common.model.Document;
import com.marsreg.search.config.TestConfig;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.impl.DocumentIndexSyncServiceImpl;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
public class DocumentIndexSyncServiceTest {

    @Autowired
    private DocumentIndexSyncServiceImpl documentIndexSyncService;

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    @MockBean
    private VectorizationService vectorizationService;

    @MockBean
    private VectorStorageService vectorStorageService;

    private static final String TEST_DOC_ID = "test-doc-1";
    private static final float[] TEST_VECTOR = new float[384];

    @BeforeEach
    void setUp() {
        // 初始化测试向量
        Arrays.fill(TEST_VECTOR, 0.1f);
        
        // 清空文档索引
        documentIndexRepository.deleteAll();
        
        // 设置向量化服务模拟行为
        when(vectorizationService.vectorize(anyString())).thenReturn(TEST_VECTOR);
    }

    @Test
    void testSyncDocument() {
        // 准备测试文档
        Document document = Document.builder()
            .id(TEST_DOC_ID)
            .title("测试文档")
            .content("这是一个测试文档的内容")
            .build();
        
        // 执行同步
        documentIndexSyncService.indexDocument(document);
        
        // 验证索引
        DocumentIndex index = documentIndexRepository.findById(TEST_DOC_ID).orElse(null);
        assertNotNull(index);
        assertEquals(TEST_DOC_ID, index.getDocumentId());
        assertEquals("测试文档", index.getTitle());
        assertEquals("这是一个测试文档的内容", index.getContent());
        
        // 验证服务调用
        verify(vectorizationService).vectorize(document.getContent());
        verify(vectorStorageService).storeVector(TEST_DOC_ID, TEST_VECTOR);
    }

    @Test
    void testDeleteDocument() {
        // 准备测试索引
        DocumentIndex index = DocumentIndex.builder()
            .id(TEST_DOC_ID)
            .documentId(TEST_DOC_ID)
            .title("测试文档")
            .content("这是一个测试文档的内容")
            .build();
        documentIndexRepository.save(index);
        
        // 执行删除
        documentIndexSyncService.deleteDocument(TEST_DOC_ID);
        
        // 验证索引已删除
        assertFalse(documentIndexRepository.existsById(TEST_DOC_ID));
        
        // 验证服务调用
        verify(vectorStorageService).deleteVector(TEST_DOC_ID);
    }

    @Test
    void testBatchSyncDocuments() {
        // 准备测试文档
        Document doc1 = Document.builder()
            .id("test-doc-1")
            .title("测试文档1")
            .content("这是测试文档1的内容")
            .build();
        
        Document doc2 = Document.builder()
            .id("test-doc-2")
            .title("测试文档2")
            .content("这是测试文档2的内容")
            .build();
        
        List<Document> documents = Arrays.asList(doc1, doc2);
        
        // 执行批量同步
        for (Document doc : documents) {
            documentIndexSyncService.indexDocument(doc);
        }
        
        // 验证索引
        List<DocumentIndex> indices = StreamSupport.stream(documentIndexRepository.findAll().spliterator(), false)
            .collect(Collectors.toList());
        assertEquals(2, indices.size());
        
        // 验证服务调用
        verify(vectorizationService, times(2)).vectorize(anyString());
        verify(vectorStorageService, times(2)).storeVector(anyString(), any());
    }

    @Test
    void testRebuildIndex() {
        // 准备测试文档
        Document doc1 = Document.builder()
            .id("test-doc-1")
            .title("测试文档1")
            .content("这是测试文档1的内容")
            .build();
        
        Document doc2 = Document.builder()
            .id("test-doc-2")
            .title("测试文档2")
            .content("这是测试文档2的内容")
            .build();
        
        // 执行重建索引
        documentIndexSyncService.reindexAll();
        
        // 验证索引
        List<DocumentIndex> indices = StreamSupport.stream(documentIndexRepository.findAll().spliterator(), false)
            .collect(Collectors.toList());
        assertEquals(0, indices.size());
        
        // 验证服务调用
        verify(vectorStorageService).deleteVectors(anyList());
    }
} 