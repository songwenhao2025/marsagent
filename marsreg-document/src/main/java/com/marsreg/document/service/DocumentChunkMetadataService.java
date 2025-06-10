package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentChunkMetadata;

import java.util.List;
import java.util.Map;

public interface DocumentChunkMetadataService {
    /**
     * 保存元数据
     * @param metadata 元数据
     * @return 保存后的元数据
     */
    DocumentChunkMetadata save(DocumentChunkMetadata metadata);

    /**
     * 批量保存元数据
     * @param metadataList 元数据列表
     * @return 保存后的元数据列表
     */
    List<DocumentChunkMetadata> saveAll(List<DocumentChunkMetadata> metadataList);

    /**
     * 根据分块ID获取元数据
     * @param chunkId 分块ID
     * @return 元数据列表
     */
    List<DocumentChunkMetadata> getByChunkId(Long chunkId);

    /**
     * 根据文档ID获取元数据
     * @param documentId 文档ID
     * @return 元数据列表
     */
    List<DocumentChunkMetadata> getByDocumentId(Long documentId);

    /**
     * 根据分块ID和键获取元数据
     * @param chunkId 分块ID
     * @param key 键
     * @return 元数据
     */
    DocumentChunkMetadata getByChunkIdAndKey(Long chunkId, String key);

    /**
     * 根据文档ID和键获取元数据
     * @param documentId 文档ID
     * @param key 键
     * @return 元数据列表
     */
    List<DocumentChunkMetadata> getByDocumentIdAndKey(Long documentId, String key);

    /**
     * 根据分块ID获取元数据Map
     * @param chunkId 分块ID
     * @return 元数据Map
     */
    Map<String, Object> getMetadataMapByChunkId(Long chunkId);

    /**
     * 根据文档ID获取元数据Map
     * @param documentId 文档ID
     * @return 元数据Map
     */
    Map<String, Object> getMetadataMapByDocumentId(Long documentId);

    /**
     * 删除元数据
     * @param metadata 元数据
     */
    void delete(DocumentChunkMetadata metadata);

    /**
     * 根据分块ID删除元数据
     * @param chunkId 分块ID
     */
    void deleteByChunkId(Long chunkId);

    /**
     * 根据文档ID删除元数据
     * @param documentId 文档ID
     */
    void deleteByDocumentId(Long documentId);

    /**
     * 根据分块ID和键删除元数据
     * @param chunkId 分块ID
     * @param key 键
     */
    void deleteByChunkIdAndKey(Long chunkId, String key);

    /**
     * 根据文档ID和键删除元数据
     * @param documentId 文档ID
     * @param key 键
     */
    void deleteByDocumentIdAndKey(Long documentId, String key);

    void saveMetadata(Long chunkId, String key, String value, String type, String description);

    List<DocumentChunkMetadata> findByChunkId(Long chunkId);
} 