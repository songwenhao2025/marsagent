package com.marsreg.document.service.impl;

import com.marsreg.common.annotation.Log;
import com.marsreg.common.annotation.Cache;
import com.marsreg.common.exception.BusinessException;
import com.marsreg.document.dto.DocumentDTO;
import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentContent;
import com.marsreg.document.enums.DocumentStatus;
import com.marsreg.document.repository.DocumentContentRepository;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.DocumentProcessService;
import com.marsreg.document.service.DocumentService;
import com.marsreg.document.service.DocumentStorageService;
import com.marsreg.document.service.DocumentVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentProcessService documentProcessService;
    private final DocumentVectorService documentVectorService;

    @Override
    @Log(module = "文档管理", operation = "上传", description = "上传文档")
    @Transactional
    public DocumentDTO uploadDocument(MultipartFile file) {
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = UUID.randomUUID().toString() + extension;

        // 保存到存储服务
        documentStorageService.storeFile(file, objectName);

        // 创建文档记录
        Document document = new Document();
        document.setName(originalFilename);
        document.setOriginalName(originalFilename);
        document.setContentType(file.getContentType());
        document.setSize(file.getSize());
        document.setStoragePath(objectName);
        document.setBucket(documentStorageService.getBucketName());
        document.setObjectName(objectName);
        document.setStatus(DocumentStatus.ACTIVE);
        document.setVersion(1L);
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());

        // 保存到数据库
        document = documentRepository.save(document);

        // 转换为DTO
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setOriginalName(document.getOriginalName());
        dto.setContentType(document.getContentType());
        dto.setSize(document.getSize());
        dto.setStoragePath(document.getStoragePath());
        dto.setBucket(document.getBucket());
        dto.setObjectName(document.getObjectName());
        dto.setStatus(document.getStatus().name());
        dto.setVersion(document.getVersion().intValue());
        dto.setCreateTime(document.getCreateTime());
        dto.setUpdateTime(document.getUpdateTime());

        return dto;
    }

    @Override
    @Log(module = "文档管理", operation = "查询", description = "查询文档详情")
    @Cache(name = "document", key = "#id", expire = 3600)
    public Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在"));
    }

    @Override
    @Log(module = "文档管理", operation = "删除", description = "删除文档")
    @Transactional
    public void deleteDocument(Long id) {
        Document document = getDocument(id);
        
        try {
            // 1. 删除存储的文件
            documentStorageService.deleteFile(document.getBucket(), document.getObjectName());
            
            // 2. 删除文档的向量
            documentVectorService.deleteDocumentVectors(id);
            
            // 3. 删除数据库记录
            documentRepository.delete(document);
            
            log.info("文档删除成功: id={}", id);
        } catch (Exception e) {
            log.error("文档删除失败: id={}", id, e);
            throw new BusinessException("文档删除失败: " + e.getMessage());
        }
    }

    @Override
    public Page<Document> listDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }

    @Override
    public String getDocumentUrl(Long id, int expirySeconds) {
        Document document = getDocument(id);
        return documentStorageService.getFileUrl(document.getBucket(), document.getObjectName(), expirySeconds);
    }

    @Override
    public String getContent(Long id) {
        return documentContentRepository.findByDocumentId(id)
                .map(DocumentContent::getContent)
                .orElseThrow(() -> new BusinessException("文档内容不存在"));
    }

    private void processDocument(Document document) {
        try {
            // 提取文本
            String text = documentProcessService.extractText(document);
            
            // 清洗文本
            String cleanedText = documentProcessService.cleanText(text);
            
            // 检测语言
            String language = documentProcessService.detectLanguage(cleanedText);
            
            // 智能分块
            List<String> chunks = documentProcessService.smartChunkText(cleanedText, 1000, 200);
            
            // 创建文档内容
            DocumentContent content = new DocumentContent();
            content.setDocumentId(document.getId());
            content.setContent(text);
            
            // 保存文档内容
            documentContentRepository.save(content);
            
            // 更新文档状态
            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);
        } catch (Exception e) {
            log.error("文档处理失败", e);
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            throw new BusinessException("文档处理失败: " + e.getMessage());
        }
    }

    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\\s+").length;
    }

    private int countParagraphs(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\n\\s*\n").length;
    }
} 