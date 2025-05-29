package com.marsreg.search.service.impl;

import com.marsreg.document.model.Document;
import com.marsreg.document.service.DocumentService;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.DocumentIndexSyncService;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexSyncServiceImpl implements DocumentIndexSyncService {

    private final DocumentService documentService;
    private final DocumentIndexRepository documentIndexRepository;
    private final VectorizationService vectorizationService;
    private final VectorStorageService vectorStorageService;

    @Override
    @Transactional
    public void syncDocument(Document document) {
        try {
            // 将文档转换为索引对象
            DocumentIndex index = convertToIndex(document);
            
            // 保存到 Elasticsearch
            documentIndexRepository.save(index);
            
            // 生成文档向量并存储
            float[] vector = vectorizationService.vectorize(document.getContent());
            vectorStorageService.store(document.getId(), vector);
            
            log.info("Successfully synced document to index: {}", document.getId());
        } catch (Exception e) {
            log.error("Failed to sync document to index: " + document.getId(), e);
            throw new RuntimeException("Failed to sync document to index", e);
        }
    }

    @Override
    @Transactional
    public void deleteDocument(String documentId) {
        try {
            // 从 Elasticsearch 中删除
            documentIndexRepository.deleteById(documentId);
            
            // 从向量存储中删除
            vectorStorageService.delete(documentId);
            
            log.info("Successfully deleted document from index: {}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete document from index: " + documentId, e);
            throw new RuntimeException("Failed to delete document from index", e);
        }
    }

    @Override
    @Transactional
    public void batchSyncDocuments(Iterable<Document> documents) {
        List<DocumentIndex> indices = new ArrayList<>();
        for (Document document : documents) {
            try {
                // 转换为索引对象
                DocumentIndex index = convertToIndex(document);
                indices.add(index);
                
                // 生成文档向量并存储
                float[] vector = vectorizationService.vectorize(document.getContent());
                vectorStorageService.store(document.getId(), vector);
            } catch (Exception e) {
                log.error("Failed to process document for batch sync: " + document.getId(), e);
            }
        }
        
        // 批量保存到 Elasticsearch
        documentIndexRepository.saveAll(indices);
        log.info("Successfully batch synced {} documents to index", indices.size());
    }

    @Override
    @Transactional
    public void rebuildIndex() {
        try {
            // 清空现有索引
            documentIndexRepository.deleteAll();
            vectorStorageService.deleteByPrefix("");
            
            // 分页获取所有文档
            int pageSize = 100;
            int pageNumber = 0;
            Page<Document> page;
            
            do {
                page = documentService.listDocuments(PageRequest.of(pageNumber, pageSize));
                batchSyncDocuments(page.getContent());
                pageNumber++;
            } while (page.hasNext());
            
            log.info("Successfully rebuilt index");
        } catch (Exception e) {
            log.error("Failed to rebuild index", e);
            throw new RuntimeException("Failed to rebuild index", e);
        }
    }

    private DocumentIndex convertToIndex(Document document) {
        DocumentIndex index = new DocumentIndex();
        index.setId(document.getId());
        index.setDocumentId(document.getId());
        index.setTitle(document.getTitle());
        index.setContent(document.getContent());
        index.setMetadata(document.getMetadata());
        index.setCreateTime(document.getCreateTime());
        index.setUpdateTime(document.getUpdateTime());
        return index;
    }
} 