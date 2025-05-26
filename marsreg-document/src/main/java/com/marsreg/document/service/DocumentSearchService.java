package com.marsreg.document.service;

import com.marsreg.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface DocumentSearchService {
    /**
     * 搜索文档
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    Page<Document> search(String query, Pageable pageable);

    /**
     * 搜索文档内容
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    Page<Document> searchContent(String query, Pageable pageable);

    /**
     * 搜索文档标题
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    Page<Document> searchTitle(String query, Pageable pageable);

    /**
     * 搜索文档并高亮
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果，包含高亮片段
     */
    Page<Map<String, Object>> searchWithHighlight(String query, Pageable pageable);

    /**
     * 搜索文档内容并高亮
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果，包含高亮片段
     */
    Page<Map<String, Object>> searchContentWithHighlight(String query, Pageable pageable);

    /**
     * 搜索文档标题并高亮
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果，包含高亮片段
     */
    Page<Map<String, Object>> searchTitleWithHighlight(String query, Pageable pageable);
} 