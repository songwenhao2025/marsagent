package com.marsreg.document.event;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.service.DocumentIndexService;
import com.marsreg.document.service.DocumentVectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentEventListener {

    @Autowired
    private DocumentIndexService documentIndexService;

    @Autowired
    private DocumentVectorService documentVectorService;

    @EventListener
    public void handleDocumentCreatedEvent(DocumentCreatedEvent event) {
        DocumentEntity document = event.getDocument();
        documentIndexService.indexDocument(document, java.util.Arrays.asList("name", "originalName", "content", "contentType", "size", "md5", "status", "objectName"));
        documentVectorService.generateVector(document);
    }

    @EventListener
    public void handleDocumentUpdatedEvent(DocumentUpdatedEvent event) {
        DocumentEntity document = event.getDocument();
        documentIndexService.updateIndex(document, java.util.Arrays.asList("name", "originalName", "content", "contentType", "size", "md5", "status", "objectName"));
        documentVectorService.generateVector(document);
    }

    @EventListener
    public void handleDocumentDeletedEvent(DocumentDeletedEvent event) {
        Long documentId = event.getDocumentId();
        documentIndexService.deleteIndex(documentId);
    }
} 