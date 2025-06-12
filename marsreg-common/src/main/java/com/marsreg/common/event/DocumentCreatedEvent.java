package com.marsreg.common.event;

import com.marsreg.common.model.Document;
import lombok.Getter;

@Getter
public class DocumentCreatedEvent extends BaseDocumentEvent {
    private final Document document;
    
    public DocumentCreatedEvent(Document document) {
        super(document, document.getId());
        this.document = document;
    }
} 