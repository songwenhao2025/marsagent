package com.marsreg.search.listener;

import com.marsreg.common.event.DocumentCreatedEvent;
import com.marsreg.common.event.DocumentUpdatedEvent;
import com.marsreg.common.event.DocumentDeletedEvent;
import com.marsreg.search.service.DocumentIndexSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentEventListener {
    
    @Autowired
    private DocumentIndexSyncService documentIndexSyncService;

    @EventListener
    public void handleDocumentCreated(DocumentCreatedEvent event) {
        documentIndexSyncService.indexDocument(event.getDocument());
    }

    @EventListener
    public void handleDocumentUpdated(DocumentUpdatedEvent event) {
        documentIndexSyncService.updateDocument(event.getNewDocument());
    }

    @EventListener
    public void handleDocumentDeleted(DocumentDeletedEvent event) {
        documentIndexSyncService.deleteDocument(event.getDocument().getId());
    }
} 