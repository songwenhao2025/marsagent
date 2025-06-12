package com.marsreg.document.event;

import com.marsreg.common.event.BaseDocumentEventListener;
import com.marsreg.common.event.DocumentCreatedEvent;
import com.marsreg.common.event.DocumentDeletedEvent;
import com.marsreg.common.event.DocumentUpdatedEvent;
import com.marsreg.document.service.DocumentEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventListener extends BaseDocumentEventListener {
    private final DocumentEventService documentEventService;

    @Override
    protected void handleDocumentCreated(DocumentCreatedEvent event) {
        log.info("Document created: {}", event.getDocumentId());
        documentEventService.handleDocumentCreated(event.getDocumentId());
    }

    @Override
    protected void handleDocumentUpdated(DocumentUpdatedEvent event) {
        log.info("Document updated: {}", event.getDocumentId());
        documentEventService.handleDocumentUpdated(event.getDocumentId());
    }

    @Override
    protected void handleDocumentDeleted(DocumentDeletedEvent event) {
        log.info("Document deleted: {}", event.getDocumentId());
        documentEventService.handleDocumentDeleted(event.getDocumentId());
    }
} 