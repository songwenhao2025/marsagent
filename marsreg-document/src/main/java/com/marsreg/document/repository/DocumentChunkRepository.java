package com.marsreg.document.repository;

import com.marsreg.document.entity.DocumentChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    
    /**
     * 根据文档ID查找所有分块
     */
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    /**
     * 根据文档ID分页查找分块
     */
    Page<DocumentChunk> findByDocumentId(Long documentId, Pageable pageable);

    /**
     * 根据文档ID删除所有分块
     */
    void deleteByDocumentId(Long documentId);

    /**
     * 根据文档ID和分块索引查找分块
     */
    DocumentChunk findByDocumentIdAndChunkIndex(Long documentId, Integer chunkIndex);

    /**
     * 根据文档ID和语言查找分块
     */
    List<DocumentChunk> findByDocumentIdAndLanguage(Long documentId, String language);

    /**
     * 根据文档ID统计分块数量
     */
    long countByDocumentId(Long documentId);

    /**
     * 根据文档ID和关键词搜索分块
     */
    @Query("SELECT c FROM DocumentChunk c WHERE c.documentId = :documentId AND c.content LIKE %:keyword%")
    List<DocumentChunk> searchByKeyword(@Param("documentId") Long documentId, @Param("keyword") String keyword);
} 