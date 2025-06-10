package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import com.marsreg.document.model.DocumentSearchRequest;
import com.marsreg.document.model.DocumentSearchResponse;

public interface DocumentSearchService {
    /**
     * 搜索文档
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    Page<DocumentEntity> search(String keyword, Pageable pageable);

    /**
     * 高级搜索
     * @param criteria 搜索条件
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    Page<DocumentEntity> advancedSearch(Map<String, Object> criteria, Pageable pageable);

    /**
     * 相似文档搜索
     * @param documentId 文档ID
     * @param pageable 分页参数
     * @return 相似文档分页结果
     */
    Page<DocumentEntity> findSimilar(Long documentId, Pageable pageable);

    /**
     * 内容搜索
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    Page<DocumentEntity> searchContent(String query, Pageable pageable);

    /**
     * 标题搜索
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    Page<DocumentEntity> searchTitle(String query, Pageable pageable);

    /**
     * 带高亮的搜索
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果，包含高亮片段
     */
    Page<Map<String, Object>> searchWithHighlight(String query, Pageable pageable);

    /**
     * 带高亮的内容搜索
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果，包含高亮片段
     */
    Page<Map<String, Object>> searchContentWithHighlight(String query, Pageable pageable);

    /**
     * 带高亮的标题搜索
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页结果，包含高亮片段
     */
    Page<Map<String, Object>> searchTitleWithHighlight(String query, Pageable pageable);

    /**
     * 向量搜索
     * @param query 搜索查询
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，包含相似度分数
     */
    List<Map<String, Object>> vectorSearch(String query, int limit, float minScore);

    /**
     * 混合搜索（关键词 + 向量）
     * @param query 搜索查询
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    List<Map<String, Object>> hybridSearch(String query, int limit, float minScore);

    /**
     * 获取搜索建议
     * @param prefix 搜索前缀
     * @param limit 建议数量限制
     * @return 建议列表
     */
    List<String> getSuggestions(String prefix, int limit);

    /**
     * 获取标题搜索建议
     * @param prefix 搜索前缀
     * @param limit 建议数量限制
     * @return 建议列表
     */
    List<String> getTitleSuggestions(String prefix, int limit);

    /**
     * 获取内容搜索建议
     * @param prefix 搜索前缀
     * @param limit 建议数量限制
     * @return 建议列表
     */
    List<String> getContentSuggestions(String prefix, int limit);

    /**
     * 获取个性化搜索建议
     * @param userId 用户ID
     * @param prefix 搜索前缀
     * @param limit 建议数量限制
     * @return 建议列表
     */
    List<String> getPersonalizedSuggestions(String userId, String prefix, int limit);

    /**
     * 获取热门搜索建议
     * @param limit 建议数量限制
     * @return 建议列表
     */
    List<String> getHotSuggestions(int limit);

    /**
     * 记录搜索建议使用情况
     * @param suggestion 被使用的建议
     * @param userId 用户ID
     */
    void recordSuggestionUsage(String suggestion, String userId);

    /**
     * 搜索文档
     * @param request 搜索请求
     * @return 搜索响应
     */
    DocumentSearchResponse searchDocuments(DocumentSearchRequest request);
} 