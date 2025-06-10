package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.enums.DocumentStatus;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DocumentSearchServiceTest {

    @Autowired
    private DocumentSearchService documentSearchService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private VectorizationService vectorizationService;

    @Autowired
    private VectorStorageService vectorStorageService;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        documentRepository.deleteAll();
    }

    private DocumentEntity createTestDocument(String name, String content) {
        DocumentEntity document = new DocumentEntity();
        document.setName(name);
        document.setOriginalName(name);
        document.setContentType("text/plain");
        document.setSize((long) content.length());
        document.setStatus(DocumentStatus.ACTIVE);
        return document;
    }

    @Test
    void testSearch() {
        // 创建测试文档
        DocumentEntity doc1 = createTestDocument("测试文档1", "这是一个测试文档");
        DocumentEntity doc2 = createTestDocument("测试文档2", "这是另一个测试文档");
        documentRepository.saveAll(Arrays.asList(doc1, doc2));

        // 执行搜索
        Pageable pageable = PageRequest.of(0, 10);
        Page<DocumentEntity> results = documentSearchService.search("测试", pageable);

        // 验证结果
        assertNotNull(results);
        assertEquals(2, results.getTotalElements());
    }

    @Test
    void testSearchWithHighlight() {
        // 创建测试文档
        DocumentEntity doc1 = createTestDocument("测试文档1", "这是一个测试文档");
        DocumentEntity doc2 = createTestDocument("测试文档2", "这是另一个测试文档");
        documentRepository.saveAll(Arrays.asList(doc1, doc2));

        // 执行带高亮的搜索
        Pageable pageable = PageRequest.of(0, 10);
        Page<Map<String, Object>> results = documentSearchService.searchWithHighlight("测试", pageable);

        // 验证结果
        assertNotNull(results);
        assertEquals(2, results.getTotalElements());
        
        // 验证高亮结果
        results.getContent().forEach(result -> {
            assertTrue(result.containsKey("highlights"));
            @SuppressWarnings("unchecked")
            Map<String, String[]> highlights = (Map<String, String[]>) result.get("highlights");
            assertTrue(highlights.containsKey("title") || highlights.containsKey("content"));
        });
    }

    @Test
    void testSearchWithPagination() {
        // 创建测试文档
        List<DocumentEntity> documents = Arrays.asList(
            createTestDocument("测试文档1", "这是第一个测试文档"),
            createTestDocument("测试文档2", "这是第二个测试文档"),
            createTestDocument("测试文档3", "这是第三个测试文档")
        );
        documentRepository.saveAll(documents);

        // 执行分页搜索
        Pageable pageable = PageRequest.of(0, 2);
        Page<DocumentEntity> results = documentSearchService.search("测试", pageable);

        // 验证结果
        assertNotNull(results);
        assertEquals(2, results.getContent().size());
        assertEquals(3, results.getTotalElements());
        assertTrue(results.hasNext());
    }

    @Test
    void testSearchWithEmptyResults() {
        // 执行空结果搜索
        Pageable pageable = PageRequest.of(0, 10);
        Page<DocumentEntity> results = documentSearchService.search("不存在的关键词", pageable);

        // 验证结果
        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertEquals(0, results.getTotalElements());
    }
} 