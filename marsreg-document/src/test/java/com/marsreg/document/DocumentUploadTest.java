package com.marsreg.document;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.service.DocumentService;
import com.marsreg.document.service.DocumentStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DocumentUploadTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentStorageService documentStorageService;

    @Test
    void testUploadPdfDocument() throws IOException {
        // 创建测试文件
        Path testFile = Paths.get("src/test/resources/test.pdf");
        if (!Files.exists(testFile)) {
            Files.createFile(testFile);
            Files.write(testFile, "Test PDF content".getBytes());
        }

        // 创建 MultipartFile
        MultipartFile file = new MockMultipartFile(
            "test.pdf",
            "test.pdf",
            "application/pdf",
            Files.readAllBytes(testFile)
        );

        // 上传文档
        DocumentEntity document = documentService.upload(file);

        // 验证文档信息
        assertNotNull(document);
        assertNotNull(document.getId());
        assertEquals("test.pdf", document.getOriginalName());
        assertEquals("application/pdf", document.getContentType());
        assertTrue(document.getSize() > 0);
        assertNotNull(document.getStoragePath());
        assertNotNull(document.getBucket());
        assertNotNull(document.getObjectName());

        // 清理测试文件
        Files.deleteIfExists(testFile);
    }

    @Test
    void testUploadImageDocument() throws IOException {
        // 创建测试图片文件
        Path testFile = Paths.get("src/test/resources/test.jpg");
        if (!Files.exists(testFile)) {
            Files.createFile(testFile);
            Files.write(testFile, "Test image content".getBytes());
        }

        // 创建 MultipartFile
        MultipartFile file = new MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            Files.readAllBytes(testFile)
        );

        // 上传文档
        DocumentEntity document = documentService.upload(file);

        // 验证文档信息
        assertNotNull(document);
        assertNotNull(document.getId());
        assertEquals("test.jpg", document.getOriginalName());
        assertEquals("image/jpeg", document.getContentType());
        assertTrue(document.getSize() > 0);
        assertNotNull(document.getStoragePath());
        assertNotNull(document.getBucket());
        assertNotNull(document.getObjectName());

        // 清理测试文件
        Files.deleteIfExists(testFile);
    }

    @Test
    void testUploadInvalidFileType() {
        // 创建不支持的文件类型
        MultipartFile file = new MockMultipartFile(
            "test.exe",
            "test.exe",
            "application/x-msdownload",
            "Test content".getBytes()
        );

        // 验证上传失败
        assertThrows(Exception.class, () -> {
            documentService.upload(file);
        });
    }

    @Test
    void testUploadLargeFile() throws IOException {
        // 创建大文件 (100MB)
        byte[] largeContent = new byte[100 * 1024 * 1024];
        Path testFile = Paths.get("src/test/resources/large.pdf");
        Files.write(testFile, largeContent);

        // 创建 MultipartFile
        MultipartFile file = new MockMultipartFile(
            "large.pdf",
            "large.pdf",
            "application/pdf",
            Files.readAllBytes(testFile)
        );

        // 验证上传失败
        assertThrows(Exception.class, () -> {
            documentService.upload(file);
        });

        // 清理测试文件
        Files.deleteIfExists(testFile);
    }

    @Test
    void testUploadRateLimit() throws IOException {
        // 创建测试文件
        Path testFile = Paths.get("src/test/resources/test.pdf");
        if (!Files.exists(testFile)) {
            Files.createFile(testFile);
            Files.write(testFile, "Test content".getBytes());
        }

        // 快速上传多个文件
        for (int i = 0; i < 15; i++) {
            MultipartFile file = new MockMultipartFile(
                "test" + i + ".pdf",
                "test" + i + ".pdf",
                "application/pdf",
                Files.readAllBytes(testFile)
            );

            if (i < 10) {
                // 前10次应该成功
                DocumentEntity document = documentService.upload(file);
                assertNotNull(document);
            } else {
                // 后5次应该因为速率限制而失败
                assertThrows(Exception.class, () -> {
                    documentService.upload(file);
                });
            }
        }

        // 清理测试文件
        Files.deleteIfExists(testFile);
    }
} 