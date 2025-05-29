package com.marsreg.search.integration;

import com.marsreg.document.model.Document;
import com.marsreg.document.service.DocumentService;
import com.marsreg.search.config.IntegrationTestConfig;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.DocumentIndexSyncService;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(IntegrationTestConfig.class)
@ActiveProfiles("test")
public class DocumentIndexSyncServiceIntegrationTest {

    @Autowired
    private DocumentIndexSyncService documentIndexSyncService;

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    @MockBean
    private DocumentService documentService;

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
    void testEndToEndSync() {
        // 准备测试文档
        Document document = Document.builder()
            .id(TEST_DOC_ID)
            .title("测试文档")
            .content("这是一个测试文档的内容")
            .build();
        
        // 执行同步
        documentIndexSyncService.syncDocument(document);
        
        // 验证索引
        DocumentIndex index = documentIndexRepository.findById(TEST_DOC_ID).orElse(null);
        assertNotNull(index);
        assertEquals(TEST_DOC_ID, index.getDocumentId());
        assertEquals("测试文档", index.getTitle());
        assertEquals("这是一个测试文档的内容", index.getContent());
        
        // 验证服务调用
        verify(vectorizationService).vectorize(document.getContent());
        verify(vectorStorageService).store(TEST_DOC_ID, TEST_VECTOR);
    }

    @Test
    void testEndToEndBatchSync() {
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
        documentIndexSyncService.batchSyncDocuments(documents);
        
        // 验证索引
        List<DocumentIndex> indices = documentIndexRepository.findAll();
        assertEquals(2, indices.size());
        
        // 验证服务调用
        verify(vectorizationService, times(2)).vectorize(anyString());
        verify(vectorStorageService, times(2)).store(anyString(), any());
    }

    @Test
    void testEndToEndRebuild() {
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
        
        // 设置文档服务模拟行为
        Page<Document> page = new PageImpl<>(Arrays.asList(doc1, doc2));
        when(documentService.listDocuments(any(PageRequest.class))).thenReturn(page);
        
        // 执行重建索引
        documentIndexSyncService.rebuildIndex();
        
        // 验证索引
        List<DocumentIndex> indices = documentIndexRepository.findAll();
        assertEquals(2, indices.size());
        
        // 验证服务调用
        verify(documentService).listDocuments(any(PageRequest.class));
        verify(vectorizationService, times(2)).vectorize(anyString());
        verify(vectorStorageService, times(2)).store(anyString(), any());
    }
} 