package com.marsreg.search.service.impl;

import com.marsreg.common.model.Document;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.DocumentIndexSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentIndexSyncServiceImpl implements DocumentIndexSyncService {

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    @Transactional
    public void indexDocument(Document document) {
        try {
            DocumentIndex index = toDocumentIndex(document);
            documentIndexRepository.save(index);
            log.info("文档已成功索引: {}", document.getId().toString());
        } catch (Exception e) {
            log.error("索引文档失败: " + document.getId().toString(), e);
            throw new RuntimeException("索引文档失败", e);
        }
    }

    @Override
    @Transactional
    public void updateDocument(Document document) {
        try {
            DocumentIndex index = toDocumentIndex(document);
            documentIndexRepository.save(index);
            log.info("文档已成功更新: {}", document.getId().toString());
        } catch (Exception e) {
            log.error("更新文档索引失败: " + document.getId().toString(), e);
            throw new RuntimeException("更新文档索引失败", e);
        }
    }

    @Override
    @Transactional
    public void deleteDocument(String documentId) {
        try {
            documentIndexRepository.deleteById(documentId);
            log.info("文档已成功从索引中删除: {}", documentId);
        } catch (Exception e) {
            log.error("从索引中删除文档失败: " + documentId, e);
            throw new RuntimeException("从索引中删除文档失败", e);
        }
    }

    @Override
    @Transactional
    public void reindexAll() {
        try {
            // 清空现有索引
            documentIndexRepository.deleteAll();
            log.info("已清空现有索引");
            
            // 重新创建索引
            elasticsearchOperations.indexOps(DocumentIndex.class).create();
            log.info("已重新创建索引");
        } catch (Exception e) {
            log.error("重建索引失败", e);
            throw new RuntimeException("重建索引失败", e);
        }
    }

    private DocumentIndex toDocumentIndex(Document document) {
        return DocumentIndex.builder()
                .documentId(document.getId().toString())
                .title(document.getName())
                .content(document.getContent())
                .documentType(document.getType())
                .status(document.getStatus())
                .contentType(document.getContentType())
                .originalName(document.getOriginalName())
                .size(document.getSize())
                .createTime(document.getCreateTime())
                .updateTime(document.getUpdateTime())
                .createBy(document.getCreateBy())
                .updateBy(document.getUpdateBy())
                .metadata(document.getMetadata())
                .build();
    }
} 