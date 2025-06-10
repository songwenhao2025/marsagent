package com.marsreg.common.event;

import com.marsreg.common.model.Document;
import lombok.Getter;

@Getter
public class DocumentDeletedEvent extends DocumentEvent {
    private final Document document;
    
    public DocumentDeletedEvent(Document document) {
        super();
        this.document = document;
        this.setDocumentId(document.getId());
        this.setOperationType("DELETE");
    }
} 