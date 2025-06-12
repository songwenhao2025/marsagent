package com.marsreg.document.service;

public interface DocumentEventService {
    /**
     * 处理文档创建事件
     * @param documentId 文档ID
     */
    void handleDocumentCreated(Long documentId);

    /**
     * 处理文档更新事件
     * @param documentId 文档ID
     */
    void handleDocumentUpdated(Long documentId);

    /**
     * 处理文档删除事件
     * @param documentId 文档ID
     */
    void handleDocumentDeleted(Long documentId);
} 