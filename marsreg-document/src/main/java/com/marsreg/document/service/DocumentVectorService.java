package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.entity.DocumentChunk;

import java.util.List;
import java.util.Map;

public interface DocumentVectorService {
    /**
     * 生成文档向量
     * @param document 文档
     * @return 文档向量
     */
    float[] generateVector(DocumentEntity document);

    /**
     * 批量生成文档向量
     * @param documents 文档列表
     * @return 文档向量映射
     */
    Map<Long, float[]> generateVectors(List<DocumentEntity> documents);

    /**
     * 计算向量相似度
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度
     */
    float calculateSimilarity(float[] vector1, float[] vector2);

    /**
     * 向量化文档分块并存储
     * @param document 文档
     * @param chunks 文档分块列表
     */
    void vectorizeAndStore(DocumentEntity document, List<DocumentChunk> chunks);

    /**
     * 批量向量化文档分块并存储
     * @param documentChunksMap 文档ID到分块列表的映射
     */
    void batchVectorizeAndStore(Map<Long, List<DocumentChunk>> documentChunksMap);

    /**
     * 删除文档的所有向量
     * @param documentId 文档ID
     */
    void deleteDocumentVectors(Long documentId);

    /**
     * 更新文档分块的向量
     * @param documentId 文档ID
     * @param chunkId 分块ID
     * @param content 分块内容
     */
    void updateChunkVector(Long documentId, Long chunkId, String content);

    /**
     * 语义搜索文档分块
     * @param query 搜索查询
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，每个结果包含分块内容和相似度分数
     */
    List<Map<String, Object>> searchChunks(String query, int limit, float minScore);

    /**
     * 在指定文档中语义搜索分块
     * @param documentId 文档ID
     * @param query 搜索查询
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，每个结果包含分块内容和相似度分数
     */
    List<Map<String, Object>> searchChunksByDocument(Long documentId, String query, int limit, float minScore);

    /**
     * 更新文档向量
     * @param document 文档实体
     */
    void updateDocumentVector(DocumentEntity document);
} 