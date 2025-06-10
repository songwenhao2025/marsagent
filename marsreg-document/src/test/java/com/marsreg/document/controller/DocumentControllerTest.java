package com.marsreg.document.controller;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.enums.DocumentStatus;
import com.marsreg.document.service.DocumentProcessService;
import com.marsreg.document.service.DocumentSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentProcessService documentProcessService;

    @Autowired
    private DocumentSearchService documentSearchService;

    private MockMultipartFile testFile;

    @BeforeEach
    void setUp() throws Exception {
        // 读取测试文件
        Path filePath = Paths.get("src/test/resources/test.pdf");
        byte[] content = Files.readAllBytes(filePath);
        testFile = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            content
        );
    }

    @Test
    void testUploadDocument() throws Exception {
        mockMvc.perform(multipart("/api/documents/upload")
                .file(testFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.pdf"))
                .andExpect(jsonPath("$.status").value(DocumentStatus.ACTIVE.name()));
    }

    @Test
    void testSearchDocuments() throws Exception {
        // 先上传一个文档
        mockMvc.perform(multipart("/api/documents/upload")
                .file(testFile))
                .andExpect(status().isOk());

        // 搜索文档
        mockMvc.perform(get("/api/documents/search")
                .param("query", "test")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetDocumentById() throws Exception {
        // 先上传一个文档
        String response = mockMvc.perform(multipart("/api/documents/upload")
                .file(testFile))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 从响应中获取文档ID
        String documentId = extractDocumentId(response);

        // 获取文档详情
        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.pdf"));
    }

    @Test
    void testDeleteDocument() throws Exception {
        // 先上传一个文档
        String response = mockMvc.perform(multipart("/api/documents/upload")
                .file(testFile))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 从响应中获取文档ID
        String documentId = extractDocumentId(response);

        // 删除文档
        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isOk());

        // 验证文档已被删除
        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUploadInvalidFile() throws Exception {
        // 创建无效文件
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "invalid content".getBytes()
        );

        // 尝试上传无效文件
        mockMvc.perform(multipart("/api/documents/upload")
                .file(invalidFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchWithInvalidParameters() throws Exception {
        // 使用无效参数搜索
        mockMvc.perform(get("/api/documents/search")
                .param("query", "")
                .param("page", "-1")
                .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetNonExistentDocument() throws Exception {
        // 尝试获取不存在的文档
        mockMvc.perform(get("/api/documents/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteNonExistentDocument() throws Exception {
        // 尝试删除不存在的文档
        mockMvc.perform(delete("/api/documents/999"))
                .andExpect(status().isNotFound());
    }

    private String extractDocumentId(String response) {
        // 从JSON响应中提取文档ID
        // 这里需要根据实际的响应格式来实现
        return response.split("\"id\":")[1].split(",")[0].trim();
    }
} 