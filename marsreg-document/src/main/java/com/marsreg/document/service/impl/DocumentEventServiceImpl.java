package com.marsreg.document.service.impl;

import com.marsreg.document.service.DocumentEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DocumentEventServiceImpl implements DocumentEventService {
    @Override
    public void handleDocumentCreated(Long documentId) {
        log.info("Handling document created event for document: {}", documentId);
        // 可以在这里添加文档创建后的处理逻辑
    }

    @Override
    public void handleDocumentUpdated(Long documentId) {
        log.info("Handling document updated event for document: {}", documentId);
        // 可以在这里添加文档更新后的处理逻辑
    }

    @Override
    public void handleDocumentDeleted(Long documentId) {
        log.info("Handling document deleted event for document: {}", documentId);
        // 可以在这里添加文档删除后的处理逻辑
    }
} 