package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.enums.DocumentStatus;
import com.marsreg.document.exception.DocumentProcessException;
import com.marsreg.document.exception.FileValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DocumentProcessServiceTest {

    @Autowired
    private DocumentProcessService documentProcessService;

    private DocumentEntity testDocument;
    private Path testFilePath;
    private static final int CONCURRENT_THREADS = 10;
    private static final int MAX_CONTENT_LENGTH = 1000000;

    @BeforeEach
    void setUp() throws IOException {
        // 创建测试文档
        testDocument = new DocumentEntity();
        testDocument.setName("test.pdf");
        testDocument.setOriginalName("test.pdf");
        testDocument.setContentType("application/pdf");
        testDocument.setStatus(DocumentStatus.PROCESSING);
        
        // 创建测试文件
        testFilePath = Paths.get("src/test/resources/test.pdf");
        if (!Files.exists(testFilePath.getParent())) {
            Files.createDirectories(testFilePath.getParent());
        }
        Files.write(testFilePath, "Test content".getBytes());
        
        // 读取测试文件内容
        byte[] content = Files.readAllBytes(testFilePath);
        testDocument.setContent(new String(content));
    }

    @AfterEach
    void tearDown() throws IOException {
        // 清理测试文件
        if (testFilePath != null && Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
    }

    @Test
    void testProcessDocument() {
        // 测试文档处理
        DocumentEntity processedDoc = documentProcessService.process(testDocument);
        assertNotNull(processedDoc);
        assertEquals(DocumentStatus.COMPLETED, processedDoc.getStatus());
        assertNotNull(processedDoc.getProcessedTime());
    }

    @Test
    void testProcessDocumentWithInvalidContentType() {
        // 设置无效的内容类型
        testDocument.setContentType("invalid/type");
        
        // 验证处理失败
        DocumentProcessException exception = assertThrows(DocumentProcessException.class, () -> {
            documentProcessService.process(testDocument);
        });
        
        // 验证异常信息
        assertTrue(exception.getMessage().contains("不支持的文件类型"));
        assertEquals(DocumentStatus.FAILED, testDocument.getStatus());
    }

    @Test
    void testProcessDocumentWithEmptyContent() {
        // 设置空内容
        testDocument.setContent("");
        
        // 验证处理失败
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            documentProcessService.process(testDocument);
        });
        
        // 验证异常信息
        assertTrue(exception.getMessage().contains("文件内容不能为空"));
        assertEquals(DocumentStatus.FAILED, testDocument.getStatus());
    }

    @Test
    void testProcessDocumentWithLargeContent() {
        // 设置超大内容
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < MAX_CONTENT_LENGTH + 1; i++) {
            largeContent.append("test content ");
        }
        testDocument.setContent(largeContent.toString());
        
        // 验证处理失败
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            documentProcessService.process(testDocument);
        });
        
        // 验证异常信息
        assertTrue(exception.getMessage().contains("文件内容超过最大限制"));
        assertEquals(DocumentStatus.FAILED, testDocument.getStatus());
    }

    @Test
    void testProcessDocumentWithNullDocument() {
        // 验证处理空文档
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessService.process(null);
        });
    }

    @Test
    void testProcessDocumentWithNullContentType() {
        // 设置空内容类型
        testDocument.setContentType(null);
        
        // 验证处理失败
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            documentProcessService.process(testDocument);
        });
        
        // 验证异常信息
        assertTrue(exception.getMessage().contains("文件类型不能为空"));
        assertEquals(DocumentStatus.FAILED, testDocument.getStatus());
    }

    @Test
    void testProcessDocumentConcurrently() throws InterruptedException {
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        
        // 并发处理文档
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            executorService.submit(() -> {
                try {
                    DocumentEntity doc = new DocumentEntity();
                    doc.setName("test.pdf");
                    doc.setOriginalName("test.pdf");
                    doc.setContentType("application/pdf");
                    doc.setStatus(DocumentStatus.PROCESSING);
                    doc.setContent("Test content " + Thread.currentThread().getId());
                    
                    DocumentEntity processedDoc = documentProcessService.process(doc);
                    assertNotNull(processedDoc);
                    assertEquals(DocumentStatus.COMPLETED, processedDoc.getStatus());
                } catch (Exception e) {
                    fail("并发处理失败: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executorService.shutdown();
    }

    @Test
    void testExtractText() {
        // 测试文本提取
        String text = documentProcessService.extractText(testDocument);
        assertNotNull(text);
        assertFalse(text.isEmpty());
        assertTrue(text.contains("Test content"));
    }

    @Test
    void testExtractTextWithEmptyDocument() {
        // 测试空文档提取
        testDocument.setContent("");
        assertThrows(FileValidationException.class, () -> {
            documentProcessService.extractText(testDocument);
        });
    }

    @Test
    void testCleanText() {
        // 测试文本清洗
        String dirtyText = "  This is a test text  with  extra  spaces  ";
        String cleanedText = documentProcessService.cleanText(dirtyText);
        assertNotNull(cleanedText);
        assertFalse(cleanedText.contains("  ")); // 不应该包含多个空格
        assertEquals("This is a test text with extra spaces", cleanedText);
    }

    @Test
    void testCleanTextWithNullInput() {
        // 测试空输入清洗
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessService.cleanText(null);
        });
    }

    @Test
    void testDetectLanguage() {
        // 测试语言检测
        String text = "这是一个测试文本";
        String language = documentProcessService.detectLanguage(text);
        assertNotNull(language);
        assertEquals("zh-CN", language);
    }

    @Test
    void testDetectLanguageWithEmptyText() {
        // 测试空文本语言检测
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessService.detectLanguage("");
        });
    }

    @Test
    void testChunkText() {
        // 测试文本分块
        String text = "This is a test text. It has multiple sentences. Each sentence should be in its own chunk.";
        List<String> chunks = documentProcessService.chunkText(text, 50, 10);
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);
        
        // 验证每个分块的大小
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 50);
        }
    }

    @Test
    void testChunkTextWithInvalidParameters() {
        // 测试无效参数
        String text = "Test text";
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessService.chunkText(text, 0, 10);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessService.chunkText(text, 50, 0);
        });
    }

    @Test
    void testSmartChunkText() {
        // 测试智能文本分块
        String text = "This is a test text. It has multiple sentences. Each sentence should be in its own chunk.";
        List<String> chunks = documentProcessService.smartChunkText(text, 50, 10);
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);
        
        // 验证每个分块的大小和完整性
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 50);
            assertTrue(chunk.matches(".*[.!?]\\s*$")); // 确保分块以句子结束
        }
    }

    @Test
    void testSmartChunkTextWithInvalidParameters() {
        // 测试无效参数
        String text = "Test text";
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessService.smartChunkText(text, 0, 10);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessService.smartChunkText(text, 50, 0);
        });
    }
} 