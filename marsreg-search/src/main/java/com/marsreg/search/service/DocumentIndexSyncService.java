package com.marsreg.search.service;

import com.marsreg.document.model.Document;

public interface DocumentIndexSyncService {
    /**
     * 同步文档到索引
     *
     * @param document 文档
     */
    void syncDocument(Document document);

    /**
     * 从索引中删除文档
     *
     * @param documentId 文档ID
     */
    void deleteDocument(String documentId);

    /**
     * 批量同步文档到索引
     *
     * @param documents 文档列表
     */
    void batchSyncDocuments(Iterable<Document> documents);

    /**
     * 重建索引
     */
    void rebuildIndex();
} 