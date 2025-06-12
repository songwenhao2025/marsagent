package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.entity.DocumentChunk;

import java.util.List;
import java.util.Map;

/**
 * 文档向量服务接口
 * 负责文档内容的向量化、存储和检索
 */
public interface DocumentVectorService {
    /**
     * 向量化并存储文档及其分块
     * @param document 文档实体
     * @param chunks 文档分块列表
     */
    void vectorizeAndStore(DocumentEntity document, List<DocumentChunk> chunks);

    /**
     * 生成文档向量
     * @param document 文档实体
     * @return 文档的向量表示
     */
    float[] generateVector(DocumentEntity document);

    /**
     * 批量生成文档向量
     * @param documents 文档实体列表
     * @return 文档ID到向量的映射
     */
    Map<Long, float[]> generateVectors(List<DocumentEntity> documents);

    /**
     * 计算两个向量的相似度
     * @param vector1 第一个向量
     * @param vector2 第二个向量
     * @return 相似度分数
     */
    float calculateSimilarity(float[] vector1, float[] vector2);

    /**
     * 批量向量化并存储文档分块
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
     * 搜索相似的分块
     * @param query 查询文本
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似分块列表，包含分块ID和相似度分数
     */
    List<Map<String, Object>> searchChunks(String query, int limit, float minScore);

    /**
     * 在指定文档中搜索相似的分块
     * @param documentId 文档ID
     * @param query 查询文本
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似分块列表，包含分块ID和相似度分数
     */
    List<Map<String, Object>> searchChunksByDocument(Long documentId, String query, int limit, float minScore);

    /**
     * 更新文档向量
     * @param document 文档实体
     */
    void updateDocumentVector(DocumentEntity document);

    /**
     * 删除文档向量
     * @param documentId 文档ID
     */
    void deleteVector(Long documentId);

    /**
    * 获取文档向量
     * @param documentId 文档ID
    * @return 文档的向量表示
    */
    List<Float> getVector(Long documentId);

    /**
    * 搜索相似文档
    * @param documentId 文档ID
    * @param topK 返回结果数量
    * @return 相似文档ID列表
    */
    List<Long> searchSimilarDocuments(Long documentId, int topK);
}