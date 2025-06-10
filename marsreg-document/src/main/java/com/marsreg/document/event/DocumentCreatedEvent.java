package com.marsreg.document.event;

import com.marsreg.document.entity.DocumentEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DocumentCreatedEvent extends ApplicationEvent {
    private final DocumentEntity document;

    public DocumentCreatedEvent(Object source, DocumentEntity document) {
        super(source);
        this.document = document;
    }
} 