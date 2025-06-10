package com.marsreg.document.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DocumentDeletedEvent extends ApplicationEvent {
    private final Long documentId;

    public DocumentDeletedEvent(Object source, Long documentId) {
        super(source);
        this.documentId = documentId;
    }
} 