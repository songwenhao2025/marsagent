package com.marsreg.document.service.impl;

import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentChunk;
import com.marsreg.document.repository.DocumentChunkRepository;
import com.marsreg.document.service.DocumentVectorService;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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

    @Override
    @Transactional
    public void vectorizeAndStore(Document document, List<DocumentChunk> chunks) {
        try {
            // 1. 批量向量化分块内容
            List<String> contents = chunks.stream()
                    .map(DocumentChunk::getContent)
                    .collect(Collectors.toList());
            List<float[]> vectors = vectorizationService.batchVectorize(contents);

            // 2. 构建向量ID映射
            Map<String, float[]> vectorMap = new HashMap<>();
            for (int i = 0; i < chunks.size(); i++) {
                String vectorId = generateVectorId(document.getId(), chunks.get(i).getId());
                vectorMap.put(vectorId, vectors.get(i));
            }

            // 3. 批量存储向量
            vectorStorageService.batchStore(vectorMap);

            log.info("文档分块向量化存储成功: documentId={}, chunkCount={}", document.getId(), chunks.size());
        } catch (Exception e) {
            log.error("文档分块向量化存储失败: documentId={}", document.getId(), e);
            throw new RuntimeException("文档分块向量化存储失败", e);
        }
    }

    @Override
    @Transactional
    public void batchVectorizeAndStore(Map<Long, List<DocumentChunk>> documentChunksMap) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<Long, List<DocumentChunk>> entry : documentChunksMap.entrySet()) {
            Long documentId = entry.getKey();
            List<DocumentChunk> chunks = entry.getValue();

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Document document = new Document();
                document.setId(documentId);
                vectorizeAndStore(document, chunks);
            }, executorService);

            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Override
    @Transactional
    public void deleteDocumentVectors(Long documentId) {
        try {
            // 构建向量ID前缀
            String vectorIdPrefix = "doc_" + documentId + "_";
            
            // 删除所有以该前缀开头的向量
            vectorStorageService.deleteByPrefix(vectorIdPrefix);
            
            log.info("文档向量删除成功: documentId={}", documentId);
        } catch (Exception e) {
            log.error("文档向量删除失败: documentId={}", documentId, e);
            throw new RuntimeException("文档向量删除失败", e);
        }
    }

    @Override
    @Transactional
    public void updateChunkVector(Long documentId, Long chunkId, String content) {
        try {
            // 1. 向量化新内容
            float[] vector = vectorizationService.vectorize(content);

            // 2. 生成向量ID
            String vectorId = generateVectorId(documentId, chunkId);

            // 3. 更新向量
            vectorStorageService.updateVector(vectorId, vector);

            log.info("文档分块向量更新成功: documentId={}, chunkId={}", documentId, chunkId);
        } catch (Exception e) {
            log.error("文档分块向量更新失败: documentId={}, chunkId={}", documentId, chunkId, e);
            throw new RuntimeException("文档分块向量更新失败", e);
        }
    }

    /**
     * 生成向量ID
     * 格式：doc_{documentId}_{chunkId}
     */
    private String generateVectorId(Long documentId, Long chunkId) {
        return String.format("doc_%d_%d", documentId, chunkId);
    }

    @Override
    public List<Map<String, Object>> searchChunks(String query, int limit, float minScore) {
        try {
            // 1. 向量化查询文本
            float[] queryVector = vectorizationService.vectorize(query);

            // 2. 搜索相似向量
            List<Map.Entry<String, Float>> searchResults = vectorStorageService.search(queryVector, limit, minScore);

            // 3. 解析向量ID并获取分块内容
            return searchResults.stream()
                    .map(result -> {
                        String vectorId = result.getKey();
                        float score = result.getValue();
                        
                        // 解析文档ID和分块ID
                        String[] parts = vectorId.split("_");
                        Long documentId = Long.parseLong(parts[1]);
                        Long chunkId = Long.parseLong(parts[2]);
                        
                        // 获取分块内容
                        DocumentChunk chunk = documentChunkRepository.findById(chunkId)
                                .orElse(null);
                        
                        if (chunk != null) {
                            Map<String, Object> chunkResult = new HashMap<>();
                            chunkResult.put("documentId", documentId);
                            chunkResult.put("chunkId", chunkId);
                            chunkResult.put("content", chunk.getContent());
                            chunkResult.put("score", score);
                            chunkResult.put("chunkIndex", chunk.getChunkIndex());
                            return chunkResult;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("文档分块搜索失败: query={}", query, e);
            throw new RuntimeException("文档分块搜索失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> searchChunksByDocument(Long documentId, String query, int limit, float minScore) {
        try {
            // 1. 向量化查询文本
            float[] queryVector = vectorizationService.vectorize(query);

            // 2. 构建向量ID前缀
            String vectorIdPrefix = "doc_" + documentId + "_";

            // 3. 在指定文档范围内搜索相似向量
            List<Map.Entry<String, Float>> searchResults = vectorStorageService.searchByPrefix(
                    queryVector, vectorIdPrefix, limit, minScore);

            // 4. 解析向量ID并获取分块内容
            return searchResults.stream()
                    .map(result -> {
                        String vectorId = result.getKey();
                        float score = result.getValue();
                        
                        // 解析分块ID
                        String[] parts = vectorId.split("_");
                        Long chunkId = Long.parseLong(parts[2]);
                        
                        // 获取分块内容
                        DocumentChunk chunk = documentChunkRepository.findById(chunkId)
                                .orElse(null);
                        
                        if (chunk != null) {
                            Map<String, Object> chunkResult = new HashMap<>();
                            chunkResult.put("documentId", documentId);
                            chunkResult.put("chunkId", chunkId);
                            chunkResult.put("content", chunk.getContent());
                            chunkResult.put("score", score);
                            chunkResult.put("chunkIndex", chunk.getChunkIndex());
                            return chunkResult;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("文档分块搜索失败: documentId={}, query={}", documentId, query, e);
            throw new RuntimeException("文档分块搜索失败", e);
        }
    }
} 