package com.marsreg.document.integration;

import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.RagSystemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RagSystemIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private RagSystemService ragSystemService;

    @BeforeEach
    void setUp() throws IOException {
        documentRepository.deleteAll();
        addTestDocuments();
    }

    private void addTestDocuments() throws IOException {
        List<Document> documents = new ArrayList<>();
        // 创建测试文档
        Document doc1 = new Document();
        doc1.setId(1L);
        doc1.setName("测试文档1");
        doc1.setContent("这是一个测试文档，包含一些关键词和内容。");
        documents.add(doc1);

        Document doc2 = new Document();
        doc2.setId(2L);
        doc2.setName("测试文档2");
        doc2.setContent("这是另一个测试文档，包含不同的关键词和内容。");
        documents.add(doc2);

        documentRepository.saveAll(documents);
    }

    @Test
    void testSearch() {
        // 测试基本搜索
        Pageable pageable = PageRequest.of(0, 10);
        Page<DocumentEntity> results = ragSystemService.search("测试", pageable);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.getTotalElements());
    }

    @Test
    void testGenerateResponse() {
        // 测试生成响应
        String query = "测试文档";
        String response = ragSystemService.generateResponse(query);
        assertNotNull(response);
        assertTrue(response.length() > 0);
    }

    @Test
    void testSearchWithNoResults() {
        // 测试无结果搜索
        Pageable pageable = PageRequest.of(0, 10);
        Page<DocumentEntity> results = ragSystemService.search("不存在的关键词", pageable);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGenerateResponseWithNoResults() {
        // 测试无结果响应生成
        String query = "不存在的关键词";
        String response = ragSystemService.generateResponse(query);
        assertNotNull(response);
        assertTrue(response.contains("未找到"));
    }
} 