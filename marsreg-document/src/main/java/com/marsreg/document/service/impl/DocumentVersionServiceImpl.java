package com.marsreg.document.service.impl;

import com.marsreg.common.annotation.Log;
import com.marsreg.common.annotation.Cache;
import com.marsreg.common.exception.BusinessException;
import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentVersion;
import com.marsreg.document.repository.DocumentVersionRepository;
import com.marsreg.document.service.DocumentService;
import com.marsreg.document.service.DocumentStorageService;
import com.marsreg.document.service.DocumentVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentVersionServiceImpl implements DocumentVersionService {

    private final DocumentVersionRepository versionRepository;
    private final DocumentService documentService;
    private final DocumentStorageService storageService;

    @Override
    @Log(module = "文档版本", operation = "创建", description = "创建文档版本")
    @Transactional
    public DocumentVersion createVersion(Long documentId, String comment, String createdBy) {
        Document document = documentService.getDocument(documentId);
        
        DocumentVersion version = DocumentVersion.builder()
            .documentId(documentId)
            .version(generateVersionNumber(documentId))
            .storagePath(document.getStoragePath())
            .bucket(document.getBucket())
            .objectName(document.getObjectName())
            .size(document.getSize())
            .contentType(document.getContentType())
            .originalName(document.getOriginalName())
            .comment(comment)
            .createdBy(createdBy)
            .createdAt(LocalDateTime.now())
            .build();
            
        return versionRepository.save(version);
    }

    @Override
    @Log(module = "文档版本", operation = "查询", description = "查询文档版本")
    @Cache(name = "document_version", key = "#versionId", expire = 3600)
    public DocumentVersion getVersion(Long versionId) {
        return versionRepository.findById(versionId)
            .orElseThrow(() -> new BusinessException("文档版本不存在"));
    }

    @Override
    @Log(module = "文档版本", operation = "查询", description = "查询文档版本列表")
    public List<DocumentVersion> getVersionsByDocumentId(Long documentId) {
        return versionRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    @Override
    @Log(module = "文档版本", operation = "查询", description = "分页查询文档版本")
    public Page<DocumentVersion> getVersionsByDocumentId(Long documentId, Pageable pageable) {
        return versionRepository.findByDocumentId(documentId, pageable);
    }

    @Override
    @Log(module = "文档版本", operation = "删除", description = "删除文档版本")
    @Transactional
    public void deleteVersion(Long versionId) {
        DocumentVersion version = getVersion(versionId);
        versionRepository.delete(version);
    }

    @Override
    @Log(module = "文档版本", operation = "恢复", description = "恢复文档版本")
    @Transactional
    public DocumentVersion restoreVersion(Long versionId) {
        DocumentVersion version = getVersion(versionId);
        Document document = documentService.getDocument(version.getDocumentId());
        
        // 创建新版本
        return createVersion(document.getId(), "从版本 " + version.getVersion() + " 恢复", version.getCreatedBy());
    }

    @Override
    @Log(module = "文档版本", operation = "获取URL", description = "获取文档版本URL")
    public String getVersionUrl(Long versionId, int expirySeconds) {
        DocumentVersion version = getVersion(versionId);
        return storageService.getFileUrl(version.getBucket(), version.getObjectName(), expirySeconds);
    }

    private String generateVersionNumber(Long documentId) {
        List<DocumentVersion> versions = getVersionsByDocumentId(documentId);
        if (versions.isEmpty()) {
            return "1.0";
        }
        
        String lastVersion = versions.get(0).getVersion();
        String[] parts = lastVersion.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        
        return major + "." + (minor + 1);
    }
} 