package com.marsreg.search.listener;

import com.marsreg.document.event.DocumentCreatedEvent;
import com.marsreg.document.event.DocumentDeletedEvent;
import com.marsreg.document.event.DocumentUpdatedEvent;
import com.marsreg.document.model.Document;
import com.marsreg.search.service.DocumentIndexSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventListener {

    private final DocumentIndexSyncService documentIndexSyncService;

    @Async
    @EventListener
    public void handleDocumentCreatedEvent(DocumentCreatedEvent event) {
        try {
            log.info("Handling document created event: {}", event.getDocument().getId());
            documentIndexSyncService.syncDocument(event.getDocument());
        } catch (Exception e) {
            log.error("Failed to handle document created event: " + event.getDocument().getId(), e);
        }
    }

    @Async
    @EventListener
    public void handleDocumentUpdatedEvent(DocumentUpdatedEvent event) {
        try {
            log.info("Handling document updated event: {}", event.getDocument().getId());
            documentIndexSyncService.syncDocument(event.getDocument());
        } catch (Exception e) {
            log.error("Failed to handle document updated event: " + event.getDocument().getId(), e);
        }
    }

    @Async
    @EventListener
    public void handleDocumentDeletedEvent(DocumentDeletedEvent event) {
        try {
            log.info("Handling document deleted event: {}", event.getDocumentId());
            documentIndexSyncService.deleteDocument(event.getDocumentId());
        } catch (Exception e) {
            log.error("Failed to handle document deleted event: " + event.getDocumentId(), e);
        }
    }
} 