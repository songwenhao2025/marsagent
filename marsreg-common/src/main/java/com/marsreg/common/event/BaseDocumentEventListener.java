package com.marsreg.common.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public abstract class BaseDocumentEventListener {
    
    @EventListener
    public void handleDocumentEvent(BaseDocumentEvent event) {
        if (event instanceof DocumentCreatedEvent) {
            handleDocumentCreated((DocumentCreatedEvent) event);
        } else if (event instanceof DocumentUpdatedEvent) {
            handleDocumentUpdated((DocumentUpdatedEvent) event);
        } else if (event instanceof DocumentDeletedEvent) {
            handleDocumentDeleted((DocumentDeletedEvent) event);
        }
    }

    protected abstract void handleDocumentCreated(DocumentCreatedEvent event);
    protected abstract void handleDocumentUpdated(DocumentUpdatedEvent event);
    protected abstract void handleDocumentDeleted(DocumentDeletedEvent event);
} 