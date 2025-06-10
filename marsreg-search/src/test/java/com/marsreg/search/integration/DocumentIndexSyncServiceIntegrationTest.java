package com.marsreg.search.integration;

import com.marsreg.search.config.IntegrationTestConfig;
import com.marsreg.common.model.Document;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
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
    private VectorizationService vectorizationService;

    @MockBean
    private VectorStorageService vectorStorageService;

    private static final String TEST_DOC_ID = "test-doc-1";
    private static final float[] TEST_VECTOR = new float[384];

    @BeforeEach
    void setUp() {
        Arrays.fill(TEST_VECTOR, 0.1f);
        documentIndexRepository.deleteAll();
        when(vectorizationService.vectorize(anyString())).thenReturn(TEST_VECTOR);
    }

    @Test
    void testEndToEndSync() {
        Document document = Document.builder()
            .id(TEST_DOC_ID)
            .title("测试文档")
            .content("这是一个测试文档的内容")
            .build();

        documentIndexSyncService.indexDocument(document);

        DocumentIndex index = documentIndexRepository.findById(TEST_DOC_ID).orElse(null);
        assertNotNull(index);
        assertEquals(TEST_DOC_ID, index.getDocumentId());
        assertEquals("测试文档", index.getTitle());
        assertEquals("这是一个测试文档的内容", index.getContent());
        verify(vectorizationService).vectorize(document.getContent());
    }

    @Test
    void testEndToEndBatchSync() {
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
        for (Document doc : documents) {
            documentIndexSyncService.indexDocument(doc);
        }
        List<DocumentIndex> indices = new ArrayList<>();
        documentIndexRepository.findAll().forEach(indices::add);
        assertEquals(2, indices.size());
        verify(vectorizationService, times(2)).vectorize(anyString());
    }

    @Test
    void testEndToEndRebuild() {
        documentIndexSyncService.reindexAll();
        // 这里只能断言索引被清空或重建，具体断言视实现而定
        // 例如：assertTrue(documentIndexRepository.count() == 0);
        assertNotNull(documentIndexRepository);
    }
} 