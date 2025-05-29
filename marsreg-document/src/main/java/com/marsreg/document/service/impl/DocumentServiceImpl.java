package com.marsreg.document.service.impl;

import com.marsreg.common.annotation.Log;
import com.marsreg.common.annotation.Cache;
import com.marsreg.common.exception.BusinessException;
import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentContent;
import com.marsreg.document.entity.DocumentStatus;
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
    public Document upload(MultipartFile file) {
        try {
            // 1. 保存文件到存储服务
            String objectName = UUID.randomUUID().toString();
            documentStorageService.storeFile(file, objectName);

            // 2. 创建文档记录
            Document document = new Document();
            document.setName(file.getOriginalFilename());
            document.setOriginalName(file.getOriginalFilename());
            document.setContentType(file.getContentType());
            document.setSize(file.getSize());
            document.setStoragePath(objectName);
            document.setStatus(DocumentStatus.PENDING);
            document.setBucket(documentStorageService.getBucketName());
            document.setObjectName(objectName);
            document = documentRepository.save(document);

            // 3. 异步处理文档
            documentProcessService.process(document);

            return document;
        } catch (Exception e) {
            log.error("文档上传失败", e);
            throw new BusinessException("文档上传失败: " + e.getMessage());
        }
    }

    @Override
    @Log(module = "文档管理", operation = "查询", description = "查询文档详情")
    @Cache(name = "document", key = "#id", expire = 3600)
    public Document getById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在"));
    }

    @Override
    @Log(module = "文档管理", operation = "删除", description = "删除文档")
    @Transactional
    public void delete(Long id) {
        Document document = getById(id);
        
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
    public Page<Document> list(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }

    @Override
    public String getDocumentUrl(Long id, int expirySeconds) {
        Document document = getById(id);
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