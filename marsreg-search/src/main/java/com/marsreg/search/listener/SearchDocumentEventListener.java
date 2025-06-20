package com.marsreg.search.listener;

import com.marsreg.common.event.DocumentCreatedEvent;
import com.marsreg.common.event.DocumentUpdatedEvent;
import com.marsreg.common.event.DocumentDeletedEvent;
import com.marsreg.search.service.DocumentIndexSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchDocumentEventListener {

    private final DocumentIndexSyncService documentIndexSyncService;

    @EventListener
    public void onDocumentCreated(DocumentCreatedEvent event) {
        log.info("Received document created event: {}", event.getDocumentId());
        documentIndexSyncService.indexDocument(event.getDocument());
    }

    @EventListener
    public void onDocumentUpdated(DocumentUpdatedEvent event) {
        log.info("Received document updated event: {}", event.getDocumentId());
        documentIndexSyncService.updateDocument(event.getDocument());
    }

    @EventListener
    public void onDocumentDeleted(DocumentDeletedEvent event) {
        log.info("Received document deleted event: {}", event.getDocumentId());
        documentIndexSyncService.deleteDocument(event.getDocumentId().toString());
    }
} 