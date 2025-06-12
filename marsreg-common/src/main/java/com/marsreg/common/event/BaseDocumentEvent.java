package com.marsreg.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public abstract class BaseDocumentEvent extends ApplicationEvent {
    private final Long documentId;

    public BaseDocumentEvent(Object source, Long documentId) {
        super(source);
        this.documentId = documentId;
    }
} 