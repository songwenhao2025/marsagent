package com.marsreg.document.service;

import com.marsreg.document.entity.Document;
import java.util.List;
import java.util.Map;

public interface DocumentIndexService {
    /**
     * 索引文档
     * @param document 文档
     * @param chunks 文档分块
     */
    void indexDocument(Document document, List<String> chunks);

    /**
     * 批量索引文档
     * @param documents 文档列表
     * @param chunksMap 文档分块映射
     */
    void indexDocuments(List<Document> documents, Map<Long, List<String>> chunksMap);

    /**
     * 删除文档索引
     * @param documentId 文档ID
     */
    void deleteIndex(Long documentId);

    /**
     * 搜索文档
     * @param query 搜索查询
     * @param page 页码
     * @param size 每页大小
     * @return 文档ID列表
     */
    List<Long> search(String query, int page, int size);

    /**
     * 刷新索引
     */
    void refreshIndex();
} 