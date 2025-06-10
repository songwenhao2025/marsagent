package com.marsreg.common.event;

import com.marsreg.common.model.Document;
import lombok.Getter;

@Getter
public class DocumentUpdatedEvent extends DocumentEvent {
    private final Document oldDocument;
    private final Document newDocument;
    
    public DocumentUpdatedEvent(Document oldDocument, Document newDocument) {
        super();
        this.oldDocument = oldDocument;
        this.newDocument = newDocument;
        this.setDocumentId(newDocument.getId());
        this.setOperationType("UPDATE");
    }
} 