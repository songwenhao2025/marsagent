package com.marsreg.common.event;

import com.marsreg.common.model.Document;
import lombok.Getter;

@Getter
public class DocumentCreatedEvent extends DocumentEvent {
    private final Document document;
    
    public DocumentCreatedEvent(Document document) {
        super();
        this.document = document;
        this.setDocumentId(document.getId());
        this.setOperationType("CREATE");
    }
} 