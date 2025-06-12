package com.marsreg.common.event;

import lombok.Getter;

@Getter
public class DocumentDeletedEvent extends BaseDocumentEvent {
    public DocumentDeletedEvent(Long documentId) {
        super(null, documentId);
    }
} 