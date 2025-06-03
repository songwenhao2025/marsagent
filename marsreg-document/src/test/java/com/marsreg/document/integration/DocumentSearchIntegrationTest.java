package com.marsreg.document.integration;

import com.marsreg.document.entity.Document;
import com.marsreg.document.enums.DocumentStatus;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.DocumentIndexService;
import com.marsreg.document.service.DocumentSearchService;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DocumentSearchIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentIndexService documentIndexService;

    @Autowired
    private DocumentSearchService documentSearchService;

    @Autowired
    private VectorizationService vectorizationService;

    @Autowired
    private VectorStorageService vectorStorageService;

    private static final String TEST_INDEX_PATH = "test-index";

    @BeforeEach
    void setUp() {
        // 清理测试数据
        documentRepository.deleteAll();
        // 重建索引
        documentIndexService.rebuildIndex();
    }

    @Test
    void testFullSearchWorkflow() {
        // 1. 准备测试数据
        Document doc1 = createTestDocument("测试文档1", "这是一个关于人工智能的测试文档，包含了一些重要的技术内容。");
        Document doc2 = createTestDocument("测试文档2", "这是另一个关于机器学习的文档，讨论了一些算法实现。");
        documentRepository.saveAll(Arrays.asList(doc1, doc2));

        // 2. 索引文档
        documentIndexService.indexDocument(doc1, Arrays.asList("title", "content"));
        documentIndexService.indexDocument(doc2, Arrays.asList("title", "content"));

        // 3. 测试基础搜索
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> searchResults = documentSearchService.search("人工智能", pageable);
        assertFalse(searchResults.isEmpty());
        assertEquals(1, searchResults.getTotalElements());

        // 4. 测试高亮搜索
        Page<Map<String, Object>> highlightResults = documentSearchService.searchWithHighlight("人工智能", pageable);
        assertFalse(highlightResults.isEmpty());
        assertTrue(highlightResults.getContent().get(0).containsKey("highlights"));

        // 5. 测试向量搜索
        List<Map<String, Object>> vectorResults = documentSearchService.vectorSearch("人工智能", 10, 0.5f);
        assertFalse(vectorResults.isEmpty());

        // 6. 测试混合搜索
        List<Map<String, Object>> hybridResults = documentSearchService.hybridSearch("人工智能", 10, 0.5f);
        assertFalse(hybridResults.isEmpty());

        // 7. 测试搜索建议
        List<String> suggestions = documentSearchService.getSuggestions("人工", 5);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("人工智能")));

        // 8. 测试更新文档
        doc1.setOriginalName("更新后的测试文档1");
        documentRepository.save(doc1);
        documentIndexService.updateIndex(doc1, Arrays.asList("title", "content"));
        
        // 验证更新后的搜索结果
        searchResults = documentSearchService.search("更新后的", pageable);
        assertFalse(searchResults.isEmpty());
        assertEquals(1, searchResults.getTotalElements());

        // 9. 测试删除文档
        documentIndexService.deleteIndex(doc1.getId());
        searchResults = documentSearchService.search("更新后的", pageable);
        assertTrue(searchResults.isEmpty());
    }

    @Test
    void testSearchPerformance() {
        // 1. 准备大量测试数据
        List<Document> documents = createBulkTestDocuments(100);
        documentRepository.saveAll(documents);
        
        // 2. 批量索引
        Map<Long, List<String>> fieldsMap = documents.stream()
            .collect(Collectors.toMap(
                Document::getId,
                doc -> Arrays.asList("title", "content")
            ));
        documentIndexService.indexDocuments(documents, fieldsMap);

        // 3. 测试搜索性能
        Pageable pageable = PageRequest.of(0, 10);
        long startTime = System.currentTimeMillis();
        
        // 执行多次搜索
        for (int i = 0; i < 10; i++) {
            documentSearchService.search("测试", pageable);
            documentSearchService.vectorSearch("测试", 10, 0.5f);
            documentSearchService.hybridSearch("测试", 10, 0.5f);
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 验证性能
        assertTrue(totalTime < 5000, "搜索性能不满足要求，总耗时: " + totalTime + "ms");
    }

    private Document createTestDocument(String name, String content) {
        Document document = new Document();
        document.setName(name);
        document.setOriginalName(name);
        document.setContentType("text/plain");
        document.setSize((long) content.length());
        document.setStatus(DocumentStatus.ACTIVE);
        return document;
    }

    private List<Document> createBulkTestDocuments(int count) {
        List<Document> documents = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            documents.add(createTestDocument(
                "测试文档" + i,
                "这是第" + i + "个测试文档，包含一些测试内容。"
            ));
        }
        return documents;
    }
} 