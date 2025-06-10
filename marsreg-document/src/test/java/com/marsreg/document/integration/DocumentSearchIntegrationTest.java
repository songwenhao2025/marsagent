package com.marsreg.document.integration;

import com.marsreg.document.model.MarsregDocument;
import com.marsreg.document.model.DocumentSearchRequest;
import com.marsreg.document.model.DocumentSearchResponse;
import com.marsreg.document.repository.MarsregDocumentRepository;
import com.marsreg.document.service.MarsregDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DocumentSearchIntegrationTest {

    @Autowired
    private MarsregDocumentRepository documentRepository;

    @Autowired
    private MarsregDocumentService documentService;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        documentRepository.deleteAll();
    }

    private MarsregDocument createTestDocument(String title, String content) {
        MarsregDocument document = new MarsregDocument();
        document.setTitle(title);
        document.setContent(content);
        document.setType("测试类型");
        document.setStatus("active");
        return document;
    }

    private List<MarsregDocument> createBulkTestDocuments(int count) {
        List<MarsregDocument> documents = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            documents.add(createTestDocument(
                "测试文档" + i,
                "这是第" + i + "个测试文档，包含一些测试内容。"
            ));
        }
        return documents;
    }

    @Test
    void testSearchDocuments() {
        // 创建测试文档
        List<MarsregDocument> documents = createBulkTestDocuments(5);
        documentRepository.saveAll(documents);

        // 执行搜索
        DocumentSearchResponse results = documentService.searchDocuments(
            new DocumentSearchRequest()
                .setQuery("测试")
                .setPage(0)
                .setSize(10)
        );

        // 验证结果
        assertNotNull(results);
        assertFalse(results.getDocuments().isEmpty());
        assertEquals(5, results.getTotal());
    }

    @Test
    void testSearchDocumentsWithPagination() {
        // 创建测试文档
        List<MarsregDocument> documents = createBulkTestDocuments(10);
        documentRepository.saveAll(documents);

        // 执行分页搜索
        DocumentSearchResponse results = documentService.searchDocuments(
            new DocumentSearchRequest()
                .setQuery("测试")
                .setPage(0)
                .setSize(5)
        );

        // 验证结果
        assertNotNull(results);
        assertEquals(5, results.getDocuments().size());
        assertEquals(10, results.getTotal());
        assertTrue(results.getPage() < results.getTotal() / results.getSize());
    }

    @Test
    void testSearchDocumentsWithEmptyResults() {
        // 执行空结果搜索
        DocumentSearchResponse results = documentService.searchDocuments(
            new DocumentSearchRequest()
                .setQuery("不存在的关键词")
                .setPage(0)
                .setSize(10)
        );

        // 验证结果
        assertNotNull(results);
        assertTrue(results.getDocuments().isEmpty());
        assertEquals(0, results.getTotal());
    }

    @Test
    void testSearchDocumentsWithInvalidPage() {
        // 创建测试文档
        List<MarsregDocument> documents = createBulkTestDocuments(5);
        documentRepository.saveAll(documents);

        // 执行无效页码搜索
        assertThrows(IllegalArgumentException.class, () -> {
            documentService.searchDocuments(
                new DocumentSearchRequest()
                    .setQuery("测试")
                    .setPage(-1)
                    .setSize(10)
            );
        });
    }

    @Test
    void testSearchDocumentsWithInvalidSize() {
        // 创建测试文档
        List<MarsregDocument> documents = createBulkTestDocuments(5);
        documentRepository.saveAll(documents);

        // 执行无效页面大小搜索
        assertThrows(IllegalArgumentException.class, () -> {
            documentService.searchDocuments(
                new DocumentSearchRequest()
                    .setQuery("测试")
                    .setPage(0)
                    .setSize(0)
            );
        });
    }
} 