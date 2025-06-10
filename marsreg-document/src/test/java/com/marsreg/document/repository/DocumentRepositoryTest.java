package com.marsreg.document.repository;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.enums.DocumentStatus;
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
    void testSaveAndFindById() {
        // 创建测试文档
        DocumentEntity document = createTestDocument("测试文档", "这是一个测试文档");
        documentRepository.save(document);

        // 查找文档
        Optional<DocumentEntity> found = documentRepository.findById(document.getId());
        assertTrue(found.isPresent());
        assertEquals(document.getName(), found.get().getName());
    }

    @Test
    void testFindAll() {
        // 创建测试文档
        List<DocumentEntity> documents = Arrays.asList(
            createTestDocument("测试文档1", "这是第一个测试文档"),
            createTestDocument("测试文档2", "这是第二个测试文档")
        );
        documentRepository.saveAll(documents);

        // 查找所有文档
        List<DocumentEntity> found = documentRepository.findAll();
        assertEquals(2, found.size());
    }

    @Test
    void testDeleteById() {
        // 创建测试文档
        DocumentEntity document = createTestDocument("测试文档", "这是一个测试文档");
        documentRepository.save(document);

        // 删除文档
        documentRepository.deleteById(document.getId());
        assertFalse(documentRepository.findById(document.getId()).isPresent());
    }

    @Test
    void testFindAllWithPagination() {
        // 创建测试文档
        List<DocumentEntity> documents = Arrays.asList(
            createTestDocument("测试文档1", "这是第一个测试文档"),
            createTestDocument("测试文档2", "这是第二个测试文档"),
            createTestDocument("测试文档3", "这是第三个测试文档")
        );
        documentRepository.saveAll(documents);

        // 分页查找文档
        Pageable pageable = PageRequest.of(0, 2);
        Page<DocumentEntity> page = documentRepository.findAll(pageable);
        
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
    }
} 