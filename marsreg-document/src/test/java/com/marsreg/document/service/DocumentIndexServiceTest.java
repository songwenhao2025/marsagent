package com.marsreg.document.service;

import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.entity.DocumentStatus;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.vector.service.VectorizationService;
import java.util.stream.Collectors;
import com.marsreg.vector.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DocumentIndexServiceTest {

    @Autowired
    private DocumentIndexService documentIndexService;

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

    private Document createTestDocument(String name, String content) {
        Document document = new Document();
        document.setName(name);
        
        document.setContentType("text/plain");
        document.setSize((long) content.length());
        document.setStatus(DocumentStatus.COMPLETED);
        return document;
    }

    @Test
    void testIndexDocument() {
        // 创建测试文档
        Document document = createTestDocument("测试文档", "这是一个测试文档");
        documentRepository.save(document);

        // 索引文档
        List<String> chunks = Arrays.asList("这是一个测试文档");
        DocumentEntity entity = new DocumentEntity();
        entity.setId(document.getId());
        entity.setName(document.getName());
        entity.setContent(document.getContent());
        entity.setContentType(document.getContentType());
        entity.setSize(document.getSize());
        entity.setStatus(com.marsreg.common.enums.DocumentStatus.COMPLETED);
        documentIndexService.indexDocument(entity, chunks);

        // 验证索引结果
        assertTrue(documentIndexService.isIndexed(document.getId()));
    }

    @Test
    void testIndexDocuments() {
        // 创建测试文档
        List<Document> documents = Arrays.asList(
            createTestDocument("测试文档1", "这是第一个测试文档"),
            createTestDocument("测试文档2", "这是第二个测试文档")
        );
        documentRepository.saveAll(documents);

        // 批量索引文档
        Map<Long, List<String>> chunksMap = new HashMap<>();
        chunksMap.put(documents.get(0).getId(), Arrays.asList("这是第一个测试文档"));
        chunksMap.put(documents.get(1).getId(), Arrays.asList("这是第二个测试文档"));
        List<DocumentEntity> entities = documents.stream()
            .map(doc -> {
                DocumentEntity entity = new DocumentEntity();
                entity.setId(doc.getId());
                entity.setName(doc.getName());
                entity.setContent(doc.getContent());
                entity.setContentType(doc.getContentType());
                entity.setSize(doc.getSize());
                entity.setStatus(com.marsreg.common.enums.DocumentStatus.COMPLETED);
                return entity;
            })
            .collect(Collectors.toList());
        documentIndexService.indexDocuments(entities, chunksMap);

        // 验证索引结果
        documents.forEach(doc -> assertTrue(documentIndexService.isIndexed(doc.getId())));
    }

    @Test
    void testRebuildIndex() {
        // 创建测试文档
        List<Document> documents = Arrays.asList(
            createTestDocument("测试文档1", "这是第一个测试文档"),
            createTestDocument("测试文档2", "这是第二个测试文档")
        );
        documentRepository.saveAll(documents);

        // 重建索引
        documentIndexService.rebuildIndex();

        // 验证索引结果
        documents.forEach(doc -> assertTrue(documentIndexService.isIndexed(doc.getId())));
    }

    @Test
    void testDeleteIndex() {
        // 创建测试文档
        Document document = createTestDocument("测试文档", "这是一个测试文档");
        documentRepository.save(document);

        // 索引文档
        List<String> chunks = Arrays.asList("这是一个测试文档");
        DocumentEntity entity = new DocumentEntity();
        entity.setId(document.getId());
        entity.setName(document.getName());
        entity.setContent(document.getContent());
        entity.setContentType(document.getContentType());
        entity.setSize(document.getSize());
        entity.setStatus(com.marsreg.common.enums.DocumentStatus.COMPLETED);
        documentIndexService.indexDocument(entity, chunks);
        assertTrue(documentIndexService.isIndexed(document.getId()));

        // 删除索引
        documentIndexService.deleteIndex(document.getId());
        assertFalse(documentIndexService.isIndexed(document.getId()));
    }

    @Test
    void testIndexDocumentWithEmptyChunks() {
        // 创建测试文档
        Document document = createTestDocument("测试文档", "这是一个测试文档");
        documentRepository.save(document);

        // 索引文档（空分块）
        List<String> emptyChunks = Arrays.asList();
        
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentEntity entity = new DocumentEntity();
        entity.setId(document.getId());
        entity.setName(document.getName());
        entity.setContent(document.getContent());
        entity.setContentType(document.getContentType());
        entity.setSize(document.getSize());
        entity.setStatus(com.marsreg.common.enums.DocumentStatus.COMPLETED);
        documentIndexService.indexDocument(entity, emptyChunks);
        });
    }

    @Test
    void testIndexDocumentWithNullChunks() {
        // 创建测试文档
        Document document = createTestDocument("测试文档", "这是一个测试文档");
        documentRepository.save(document);

        // 索引文档（空分块）
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentEntity entity = new DocumentEntity();
        entity.setId(document.getId());
        entity.setName(document.getName());
        entity.setContent(document.getContent());
        entity.setContentType(document.getContentType());
        entity.setSize(document.getSize());
        entity.setStatus(com.marsreg.common.enums.DocumentStatus.COMPLETED);
        documentIndexService.indexDocument(entity, null);
        });
    }

    @Test
    void testIndexDocumentsWithEmptyMap() {
        // 创建测试文档
        List<Document> documents = Arrays.asList(
            createTestDocument("测试文档1", "这是第一个测试文档"),
            createTestDocument("测试文档2", "这是第二个测试文档")
        );
        documentRepository.saveAll(documents);

        // 批量索引文档（空映射）
        Map<Long, List<String>> emptyMap = new HashMap<>();
        
        assertThrows(IllegalArgumentException.class, () -> {
            List<DocumentEntity> entities = documents.stream()
            .map(doc -> {
                DocumentEntity entity = new DocumentEntity();
                entity.setId(doc.getId());
                entity.setName(doc.getName());
                entity.setContent(doc.getContent());
                entity.setContentType(doc.getContentType());
                entity.setSize(doc.getSize());
                entity.setStatus(com.marsreg.common.enums.DocumentStatus.COMPLETED);
                return entity;
            })
            .collect(Collectors.toList());
        documentIndexService.indexDocuments(entities, emptyMap);
        });
    }
} 