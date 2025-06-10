package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentEntity;
import java.util.List;
import java.util.Map;

public interface DocumentIndexService {
    /**
     * 索引单个文档
     * @param document 文档实体
     * @param fields 需要索引的字段列表
     */
    void indexDocument(DocumentEntity document, List<String> fields);

    /**
     * 批量索引文档
     * @param documents 文档实体列表
     * @param fieldsMap 每个文档需要索引的字段映射
     */
    void indexDocuments(List<DocumentEntity> documents, Map<Long, List<String>> fieldsMap);

    /**
     * 更新文档索引
     * @param document 文档实体
     * @param fields 需要更新的字段列表
     */
    void updateIndex(DocumentEntity document, List<String> fields);

    /**
     * 删除文档索引
     * @param documentId 文档ID
     */
    void deleteIndex(Long documentId);

    /**
     * 批量索引文档
     * @param documents 文档实体列表
     */
    void batchIndex(Iterable<DocumentEntity> documents);

    /**
     * 批量删除文档索引
     * @param documentIds 文档ID列表
     */
    void deleteIndices(List<Long> documentIds);

    /**
     * 批量更新文档索引
     * @param documents 文档列表
     * @param chunksMap 文档分块映射
     */
    void updateIndices(List<DocumentEntity> documents, Map<Long, List<String>> chunksMap);

    /**
     * 刷新索引
     */
    void refreshIndex();

    /**
     * 重建索引
     */
    void rebuildIndex();

    /**
     * 获取索引统计信息
     * @return 索引统计信息
     */
    Map<String, Object> getIndexStats();

    /**
     * 优化索引
     */
    void optimizeIndex();

    /**
     * 检查索引状态
     * @return 索引状态信息
     */
    Map<String, Object> checkIndexStatus();

    /**
     * 清除文档缓存
     * @param documentId 文档ID
     */
    void clearDocumentCache(Long documentId);

    /**
     * 检查文档是否已索引
     * @param documentId 文档ID
     * @return 是否已索引
     */
    boolean isIndexed(Long documentId);

    /**
     * 更新文档索引
     * @param document 文档实体
     */
    void updateDocumentIndex(DocumentEntity document);
} 