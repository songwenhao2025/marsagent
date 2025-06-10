package com.marsreg.search.service;

import com.marsreg.common.model.Document;

public interface DocumentIndexSyncService {
    /**
     * 同步文档到索引
     *
     * @param document 文档
     */
    void indexDocument(Document document);

    /**
     * 更新文档到索引
     *
     * @param document 文档
     */
    void updateDocument(Document document);

    /**
     * 从索引中删除文档
     *
     * @param documentId 文档ID
     */
    void deleteDocument(String documentId);

    /**
     * 重建索引
     */
    void reindexAll();
} 