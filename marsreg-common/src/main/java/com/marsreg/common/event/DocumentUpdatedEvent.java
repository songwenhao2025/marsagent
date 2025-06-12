package com.marsreg.common.event;

import com.marsreg.common.model.Document;
import lombok.Getter;

@Getter
public class DocumentUpdatedEvent extends BaseDocumentEvent {
    private final Document document;
    
    public DocumentUpdatedEvent(Document document) {
        super(document, document.getId());
        this.document = document;
    }
} 