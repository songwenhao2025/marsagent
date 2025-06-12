package com.marsreg.document.repository;

import com.marsreg.document.entity.Document;
import com.marsreg.common.enums.DocumentStatus;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

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
        document.setStatus(com.marsreg.document.entity.DocumentStatus.COMPLETED);
        return document;
    }

    @Test
    void testSaveAndFindById() {
        // 创建测试文档
        Document document = createTestDocument("测试文档", "这是一个测试文档");
        documentRepository.save(document);

        // 查找文档
        Optional<Document> found = documentRepository.findById(document.getId());
        assertTrue(found.isPresent());
        assertEquals(document.getName(), found.get().getName());
    }

    @Test
    void testFindAll() {
        // 创建测试文档
        List<Document> documents = Arrays.asList(
            createTestDocument("测试文档1", "这是第一个测试文档"),
            createTestDocument("测试文档2", "这是第二个测试文档")
        );
        documentRepository.saveAll(documents);

        // 查找所有文档
        List<Document> found = documentRepository.findAll();
        assertEquals(2, found.size());
    }

    @Test
    void testDeleteById() {
        // 创建测试文档
        Document document = createTestDocument("测试文档", "这是一个测试文档");
        documentRepository.save(document);

        // 删除文档
        documentRepository.deleteById(document.getId());
        assertFalse(documentRepository.findById(document.getId()).isPresent());
    }

    @Test
    void testFindAllWithPagination() {
        // 创建测试文档
        List<Document> documents = Arrays.asList(
            createTestDocument("测试文档1", "这是第一个测试文档"),
            createTestDocument("测试文档2", "这是第二个测试文档"),
            createTestDocument("测试文档3", "这是第三个测试文档")
        );
        documentRepository.saveAll(documents);

        // 分页查找文档
        Pageable pageable = PageRequest.of(0, 2);
        Page<Document> page = documentRepository.findAll(pageable);
        
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
    }
} 