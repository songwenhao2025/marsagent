package com.marsreg.document.service.impl;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.entity.DocumentChunk;
import com.marsreg.document.repository.DocumentChunkRepository;
import com.marsreg.document.service.DocumentVectorService;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentVectorServiceImpl implements DocumentVectorService {

    private final VectorizationService vectorizationService;
    private final VectorStorageService vectorStorageService;
    private final DocumentChunkRepository documentChunkRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    @Transactional
    public void vectorizeAndStore(DocumentEntity document, List<DocumentChunk> chunks) {
        try {
            // 生成文档向量
            float[] documentVector = vectorizationService.vectorize(document.getContent());
            document.setVector(documentVector);
            
            // 生成并存储分块向量
            for (DocumentChunk chunk : chunks) {
                float[] chunkVector = vectorizationService.vectorize(chunk.getContent());
                String vectorId = generateVectorId(document.getId(), chunk.getId());
                vectorStorageService.storeVector(vectorId, chunkVector);
            }
            
            log.info("文档向量化成功: {}", document.getId());
        } catch (Exception e) {
            log.error("文档向量化失败: " + document.getId(), e);
            throw new RuntimeException("文档向量化失败", e);
        }
    }

    @Override
    public float[] generateVector(DocumentEntity document) {
        return vectorizationService.vectorize(document.getContent());
    }

    @Override
    public Map<Long, float[]> generateVectors(List<DocumentEntity> documents) {
        return documents.stream()
            .collect(Collectors.toMap(
                DocumentEntity::getId,
                this::generateVector
            ));
    }

    @Override
    public float calculateSimilarity(float[] vector1, float[] vector2) {
        return vectorizationService.calculateSimilarity(vector1, vector2);
    }

    @Override
    @Transactional
    public void batchVectorizeAndStore(Map<Long, List<DocumentChunk>> documentChunksMap) {
        try {
            for (Map.Entry<Long, List<DocumentChunk>> entry : documentChunksMap.entrySet()) {
                Long documentId = entry.getKey();
                List<DocumentChunk> chunks = entry.getValue();
                
                for (DocumentChunk chunk : chunks) {
                    float[] chunkVector = vectorizationService.vectorize(chunk.getContent());
                    String vectorId = generateVectorId(documentId, chunk.getId());
                    vectorStorageService.storeVector(vectorId, chunkVector);
                }
            }
            
            log.info("批量文档向量化成功");
        } catch (Exception e) {
            log.error("批量文档向量化失败", e);
            throw new RuntimeException("批量文档向量化失败", e);
        }
    }

    @Override
    @Transactional
    public void deleteDocumentVectors(Long documentId) {
        try {
            // 获取文档的所有分块
            List<DocumentChunk> chunks = documentChunkRepository.findByDocumentId(documentId);
            
            // 删除所有分块向量
            for (DocumentChunk chunk : chunks) {
                String vectorId = generateVectorId(documentId, chunk.getId());
                vectorStorageService.deleteVector(vectorId);
            }
            
            log.info("文档向量删除成功: {}", documentId);
        } catch (Exception e) {
            log.error("文档向量删除失败: " + documentId, e);
            throw new RuntimeException("文档向量删除失败", e);
        }
    }

    @Override
    @Transactional
    public void updateChunkVector(Long documentId, Long chunkId, String content) {
        try {
            float[] chunkVector = vectorizationService.vectorize(content);
            String vectorId = generateVectorId(documentId, chunkId);
            vectorStorageService.storeVector(vectorId, chunkVector);
            
            log.info("分块向量更新成功: documentId={}, chunkId={}", documentId, chunkId);
        } catch (Exception e) {
            log.error("分块向量更新失败: documentId=" + documentId + ", chunkId=" + chunkId, e);
            throw new RuntimeException("分块向量更新失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> searchChunks(String query, int limit, float minScore) {
        try {
            float[] queryVector = vectorizationService.vectorize(query);
            Map<String, Float> results = vectorStorageService.searchSimilar(queryVector, limit, minScore);
            
            return results.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("chunkId", extractChunkId(entry.getKey()));
                    result.put("score", entry.getValue());
                    return result;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("分块搜索失败", e);
            throw new RuntimeException("分块搜索失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> searchChunksByDocument(Long documentId, String query, int limit, float minScore) {
        try {
            float[] queryVector = vectorizationService.vectorize(query);
            
            // 获取文档的所有分块ID
            List<DocumentChunk> chunks = documentChunkRepository.findByDocumentId(documentId);
            List<String> vectorIds = chunks.stream()
                .map(chunk -> generateVectorId(documentId, chunk.getId()))
                .collect(Collectors.toList());
            
            // 在指定范围内搜索相似向量
            Map<String, Float> results = vectorStorageService.searchSimilarInRange(queryVector, vectorIds, limit, minScore);
            
            return results.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("chunkId", extractChunkId(entry.getKey()));
                    result.put("score", entry.getValue());
                    return result;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("文档分块搜索失败: documentId=" + documentId, e);
            throw new RuntimeException("文档分块搜索失败", e);
        }
    }

    @Override
    @Transactional
    public void updateDocumentVector(DocumentEntity document) {
        try {
            float[] vector = vectorizationService.vectorize(document.getContent());
            document.setVector(vector);
            log.info("文档向量更新成功: {}", document.getId());
        } catch (Exception e) {
            log.error("文档向量更新失败: " + document.getId(), e);
            throw new RuntimeException("文档向量更新失败", e);
        }
    }

    private String generateVectorId(Long documentId, Long chunkId) {
        return documentId + "_" + chunkId;
    }

    private Long extractChunkId(String vectorId) {
        return Long.parseLong(vectorId.split("_")[1]);
    }
} 