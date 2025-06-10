package com.marsreg.document.service.impl;

import com.marsreg.common.annotation.Log;
import com.marsreg.common.annotation.Cache;
import com.marsreg.common.exception.BusinessException;
import com.marsreg.document.dto.DocumentDTO;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.entity.DocumentContent;
import com.marsreg.document.enums.DocumentStatus;
import com.marsreg.document.repository.DocumentContentRepository;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.DocumentCoreService;
import com.marsreg.document.service.DocumentService;
import com.marsreg.document.service.DocumentStorageService;
import com.marsreg.document.service.DocumentVectorService;
import com.marsreg.document.event.DocumentCreatedEvent;
import com.marsreg.document.event.DocumentDeletedEvent;
import com.marsreg.document.model.DocumentSearchRequest;
import com.marsreg.document.model.DocumentSearchResponse;
import com.marsreg.document.model.MarsregDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentCoreService documentCoreService;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentVectorService documentVectorService;

    @Override
    @Transactional
    public DocumentEntity save(DocumentEntity document) {
        return documentRepository.save(document);
    }

    @Override
    @Transactional
    public DocumentEntity upload(MultipartFile file) throws IOException {
        // 实现上传逻辑
        return null;
    }

    @Override
    @Transactional
    public List<DocumentEntity> batchUpload(List<MultipartFile> files) throws IOException {
        // 实现批量上传逻辑
        return null;
    }

    @Override
    @Log(module = "文档管理", operation = "查询", description = "查询文档详情")
    @Cache(name = "document", key = "#id", expire = 3600)
    public Optional<DocumentEntity> getDocumentEntity(Long id) {
        return documentRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        DocumentEntity document = getDocumentEntity(id)
            .orElseThrow(() -> new BusinessException("文档不存在"));
        documentStorageService.deleteFile(document.getObjectName());
        documentRepository.delete(document);
        eventPublisher.publishEvent(new DocumentDeletedEvent(this, id));
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        // 实现批量删除逻辑
    }

    @Override
    public DocumentEntity getById(Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new BusinessException("文档不存在"));
    }

    @Override
    public Page<DocumentEntity> page(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }

    @Override
    public List<DocumentDTO> listDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable)
            .getContent()
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public String getDocumentUrl(Long id, int expiration) {
        // 实现获取文档 URL 逻辑
        return null;
    }

    @Override
    public String getContent(Long id) {
        // 实现获取文档内容逻辑
        return null;
    }

    @Override
    @Transactional
    public DocumentDTO createDocument(MultipartFile file, DocumentDTO metadata) {
        // 保存文件到存储服务
        String objectName = documentStorageService.storeFile(file);
        
        // 创建文档实体
        DocumentEntity document = new DocumentEntity();
        document.setName(file.getOriginalFilename());
        document.setOriginalName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setSize(file.getSize());
        document.setObjectName(objectName);
        document.setBucket(documentStorageService.getBucket());
        document.setStoragePath(documentStorageService.getStoragePath());
        
        // 保存文档实体
        document = documentRepository.save(document);
        
        // 处理文档
        document = documentCoreService.processDocument(document);
        
        // 更新文档向量和索引
        documentCoreService.updateDocumentVector(document);
        documentCoreService.updateDocumentIndex(document);
        
        return convertToDTO(document);
    }

    @Override
    @Transactional
    public DocumentDTO updateDocument(Long id, DocumentDTO metadata) {
        DocumentEntity document = getDocumentEntity(id)
            .orElseThrow(() -> new BusinessException("文档不存在"));
            
        // 更新文档元数据
        document.setName(metadata.getName());
        document.setCategory(metadata.getCategory());
        document.setTags(metadata.getTags());
        
        // 保存更新
        document = documentRepository.save(document);
        
        // 更新文档向量和索引
        documentCoreService.updateDocumentVector(document);
        documentCoreService.updateDocumentIndex(document);
        
        return convertToDTO(document);
    }

    @Override
    public Optional<DocumentDTO> getDocument(Long id) {
        return getDocumentEntity(id)
            .map(this::convertToDTO);
    }
    
    private DocumentDTO convertToDTO(DocumentEntity entity) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setOriginalName(entity.getOriginalName());
        dto.setContentType(entity.getContentType());
        dto.setSize(entity.getSize());
        dto.setCategory(entity.getCategory());
        dto.setTags(entity.getTags());
        dto.setStatus(entity.getStatus());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setProcessedTime(entity.getProcessedTime());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        return dto;
    }
} 