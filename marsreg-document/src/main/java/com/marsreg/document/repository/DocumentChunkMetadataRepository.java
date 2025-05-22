package com.marsreg.document.repository;

import com.marsreg.document.entity.DocumentChunkMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface DocumentChunkMetadataRepository extends JpaRepository<DocumentChunkMetadata, Long> {
    
    /**
     * 根据分块ID查找所有元数据
     */
    List<DocumentChunkMetadata> findByChunkId(Long chunkId);

    /**
     * 根据文档ID查找所有元数据
     */
    List<DocumentChunkMetadata> findByDocumentId(Long documentId);

    /**
     * 根据分块ID和键查找元数据
     */
    DocumentChunkMetadata findByChunkIdAndKey(Long chunkId, String key);

    /**
     * 根据文档ID和键查找元数据
     */
    List<DocumentChunkMetadata> findByDocumentIdAndKey(Long documentId, String key);

    /**
     * 根据分块ID删除所有元数据
     */
    void deleteByChunkId(Long chunkId);

    /**
     * 根据文档ID删除所有元数据
     */
    void deleteByDocumentId(Long documentId);

    /**
     * 根据分块ID和键删除元数据
     */
    void deleteByChunkIdAndKey(Long chunkId, String key);

    /**
     * 根据文档ID和键删除元数据
     */
    void deleteByDocumentIdAndKey(Long documentId, String key);

    /**
     * 根据分块ID获取元数据Map
     */
    @Query("SELECT new map(m.key as key, m.value as value, m.type as type, m.description as description) " +
           "FROM DocumentChunkMetadata m WHERE m.chunkId = :chunkId")
    List<Map<String, Object>> findMetadataMapByChunkId(@Param("chunkId") Long chunkId);

    /**
     * 根据文档ID获取元数据Map
     */
    @Query("SELECT new map(m.key as key, m.value as value, m.type as type, m.description as description) " +
           "FROM DocumentChunkMetadata m WHERE m.documentId = :documentId")
    List<Map<String, Object>> findMetadataMapByDocumentId(@Param("documentId") Long documentId);
} 