package com.marsreg.document;

import com.marsreg.document.controller.DocumentController;
import com.marsreg.document.dto.DocumentDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DocumentUploadTest {

    @Autowired
    private DocumentController documentController;

    @Test
    void testDocumentUpload() throws Exception {
        // 创建测试文件
        String content = "这是一个测试文档。\n" +
                        "用于测试文档上传功能。\n" +
                        "包含多个段落和句子。\n" +
                        "测试文档分块和向量化功能。";
        
        Path tempFile = Files.createTempFile("test", ".txt");
        Files.write(tempFile, content.getBytes());

        // 创建 MultipartFile
        File file = tempFile.toFile();
        FileInputStream input = new FileInputStream(file);
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            file.getName(),
            "text/plain",
            input
        );

        // 上传文档
        DocumentDTO result = documentController.uploadDocument(multipartFile);
        
        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(file.getName(), result.getName());
        assertTrue(result.getSize() > 0);
        
        // 清理测试文件
        Files.delete(tempFile);
    }
} 