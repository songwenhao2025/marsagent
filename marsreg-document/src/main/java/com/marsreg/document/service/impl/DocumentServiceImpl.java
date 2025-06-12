package com.marsreg.document.service.impl;

import com.marsreg.document.dto.DocumentDTO;
import com.marsreg.document.dto.DocumentQueryDTO;
import com.marsreg.common.dto.DocumentMetadataDTO;
import com.marsreg.document.dto.DocumentChunkDTO;
import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.entity.DocumentStatus;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.DocumentService;
import com.marsreg.document.service.DocumentStorageService;
import com.marsreg.document.validation.FileValidator;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;

    @Override
    public DocumentDTO createDocument(MultipartFile file, DocumentDTO metadata) {
        FileValidator.validate(file);
        Document document = new Document();
        BeanUtils.copyProperties(metadata, document);
        document.setName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setSize(file.getSize());
        document.setStatus(DocumentStatus.PENDING);
        document = documentRepository.save(document);
        DocumentDTO dto = new DocumentDTO();
        BeanUtils.copyProperties(document, dto);
        return dto;
    }

    @Override
    public DocumentDTO updateDocument(Long id, DocumentDTO metadata) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        BeanUtils.copyProperties(metadata, document);
        document = documentRepository.save(document);
        DocumentDTO dto = new DocumentDTO();
        BeanUtils.copyProperties(document, dto);
        return dto;
    }

    @Override
    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        documentRepository.delete(document);
    }

    @Override
    public Optional<DocumentDTO> getDocument(Long id) {
        return documentRepository.findById(id)
                .map(document -> {
                    DocumentDTO dto = new DocumentDTO();
                    BeanUtils.copyProperties(document, dto);
                    return dto;
                });
    }

    @Override
    public List<DocumentDTO> listDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable)
                .stream()
                .map(document -> {
                    DocumentDTO dto = new DocumentDTO();
                    BeanUtils.copyProperties(document, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DocumentEntity> getDocumentEntity(Long id) {
        return documentRepository.findById(id)
                .map(document -> {
                    DocumentEntity entity = new DocumentEntity();
                    BeanUtils.copyProperties(document, entity);
                    return entity;
                });
    }

    @Override
    public DocumentEntity save(DocumentEntity document) {
        Document doc = new Document();
        BeanUtils.copyProperties(document, doc);
        doc = documentRepository.save(doc);
        BeanUtils.copyProperties(doc, document);
        return document;
    }

    @Override
    public DocumentEntity upload(MultipartFile file) throws IOException {
        FileValidator.validate(file);
        Document document = new Document();
        document.setName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setSize(file.getSize());
        document.setStatus(DocumentStatus.PENDING);
        document = documentRepository.save(document);
        DocumentEntity entity = new DocumentEntity();
        BeanUtils.copyProperties(document, entity);
        return entity;
    }

    @Override
    public List<DocumentEntity> batchUpload(List<MultipartFile> files) throws IOException {
        List<DocumentEntity> entities = new ArrayList<>();
        for (MultipartFile file : files) {
            Document document = new Document();
            document.setName(file.getOriginalFilename());
            document.setContentType(file.getContentType());
            document.setSize(file.getSize());
            document.setStatus(DocumentStatus.PENDING);
            document = documentRepository.save(document);
            DocumentEntity entity = new DocumentEntity();
            BeanUtils.copyProperties(document, entity);
            entities.add(entity);
        }
        return entities;
    }

    @Override
    public DocumentEntity getById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        DocumentEntity entity = new DocumentEntity();
        BeanUtils.copyProperties(document, entity);
        return entity;
    }

    @Override
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            deleteDocument(id);
        }
    }

    @Override
    public Page<DocumentEntity> page(Pageable pageable) {
        Page<Document> documents = documentRepository.findAll(pageable);
        return documents.map(document -> {
            DocumentEntity entity = new DocumentEntity();
            BeanUtils.copyProperties(document, entity);
            return entity;
        });
    }

    @Override
    public String getDocumentUrl(Long id, int expiration) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        DocumentEntity documentEntity = new DocumentEntity();
        BeanUtils.copyProperties(document, documentEntity);
        return documentStorageService.getDocumentUrl(documentEntity);
    }

    @Override
    public String getContent(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return document.getContent();
    }

    @Override
    public Page<DocumentEntity> search(DocumentQueryDTO query, Pageable pageable) {
        // TODO: 实现文档搜索逻辑
        Page<Document> documents = documentRepository.findAll(pageable);
        return documents.map(document -> {
            DocumentEntity entity = new DocumentEntity();
            BeanUtils.copyProperties(document, entity);
            return entity;
        });
    }

    @Override
    public List<DocumentChunkDTO> getChunks(Long documentId) {
        // TODO: 实现获取文档分块逻辑
        return new ArrayList<>();
    }

    @Override
    public DocumentEntity updateMetadata(Long id, DocumentMetadataDTO metadata) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        document.setName(metadata.getName());
        document.setContentType(metadata.getContentType());
        document.setStatus(DocumentStatus.valueOf(metadata.getStatus()));
        document.setDescription(metadata.getDescription());
        document.setTags(String.join(",", metadata.getTags()));
        document.setCustomMetadata(metadata.getCustomMetadata());
        document = documentRepository.save(document);
        DocumentEntity entity = new DocumentEntity();
        BeanUtils.copyProperties(document, entity);
        return entity;
    }

    @Override
    public DocumentEntity updateContent(Long id, MultipartFile file) throws IOException {
        FileValidator.validate(file);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        document.setName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setSize(file.getSize());
        document = documentRepository.save(document);
        DocumentEntity entity = new DocumentEntity();
        BeanUtils.copyProperties(document, entity);
        return entity;
    }

    @Override
    public DocumentDTO convertToDTO(DocumentEntity entity) {
        DocumentDTO dto = new DocumentDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
} 