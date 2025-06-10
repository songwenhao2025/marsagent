package com.marsreg.document.event;

import com.marsreg.document.entity.DocumentEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DocumentUpdatedEvent extends ApplicationEvent {
    private final DocumentEntity document;

    public DocumentUpdatedEvent(Object source, DocumentEntity document) {
        super(source);
        this.document = document;
    }
} 