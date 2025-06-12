package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.model.DocumentSearchRequest;
import com.marsreg.document.model.DocumentSearchResponse;

import java.util.List;

/**
 * 文档核心服务接口
 * 负责处理文档的核心业务逻辑，避免循环依赖
 */
public interface DocumentCoreService {
    
    /**
     * 处理文档
     * @param document 文档实体
     * @return 处理后的文档实体
     */
    DocumentEntity processDocument(DocumentEntity document);
    
    /**
     * 搜索文档
     * @param request 搜索请求
     * @return 搜索响应
     */
    DocumentSearchResponse searchDocuments(DocumentSearchRequest request);
    
    /**
     * 更新文档向量
     * @param document 文档实体
     */
    void updateDocumentVector(DocumentEntity document);
    
    /**
     * 更新文档索引
     * @param document 文档实体
     */
    void updateDocumentIndex(DocumentEntity document);
    
    /**
     * 删除文档索引
     * @param documentId 文档ID
     */
    void deleteDocumentIndex(Long documentId);
    
    /**
     * 提取文本
     * @param document 文档实体
     * @return 提取的文本
     */
    String extractText(DocumentEntity document);
    
    /**
     * 清洗文本
     * @param text 原始文本
     * @return 清洗后的文本
     */
    String cleanText(String text);
    
    /**
     * 检测语言
     * @param text 文本
     * @return 语言代码
     */
    String detectLanguage(String text);
    
    /**
     * 智能分块
     * @param text 文本
     * @param maxChunkSize 最大块大小
     * @param overlap 重叠大小
     * @return 文本块列表
     */
    List<String> smartChunkText(String text, int maxChunkSize, int overlap);
} 