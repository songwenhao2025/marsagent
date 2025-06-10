package com.marsreg.document.integration;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.DocumentSearchService;
import com.marsreg.document.service.DocumentVectorService;
import com.marsreg.document.service.RagSystemService;
import com.marsreg.document.service.impl.DocumentSearchServiceImpl;
import com.marsreg.document.service.impl.DocumentVectorServiceImpl;
import com.marsreg.document.service.impl.RagSystemServiceImpl;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RagSystemPerformanceTest {

    private static final int DOCUMENT_COUNT = 100;
    private static final int QUERY_COUNT = 10;
    private static final int MAX_RESULTS = 5;

    @Autowired
    private RagSystemService ragSystemService;

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        addTestDocuments();
    }

    private void addTestDocuments() {
        List<DocumentEntity> documents = new ArrayList<>();
        for (int i = 0; i < DOCUMENT_COUNT; i++) {
            DocumentEntity doc = new DocumentEntity();
            doc.setId((long) i);
            doc.setOriginalName("测试文档" + i);
            doc.setContent("这是一个测试文档，包含一些关键词和内容。" + i);
            documents.add(doc);
        }
        documentRepository.saveAll(documents);
    }

    @Test
    void testSearchPerformance() {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < QUERY_COUNT; i++) {
            String query = "测试文档" + i;
            Pageable pageable = PageRequest.of(0, MAX_RESULTS);
            Page<DocumentEntity> results = ragSystemService.search(query, pageable);
            assertNotNull(results);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("搜索性能测试完成，总耗时: " + (endTime - startTime) + "ms");
    }

    @Test
    void testRagPerformance() {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < QUERY_COUNT; i++) {
            String query = "测试文档" + i;
            String response = ragSystemService.generateResponse(query);
            assertNotNull(response);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("RAG系统性能测试完成，总耗时: " + (endTime - startTime) + "ms");
    }
} 