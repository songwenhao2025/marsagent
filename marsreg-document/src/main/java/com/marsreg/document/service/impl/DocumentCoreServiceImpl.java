package com.marsreg.document.service.impl;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.model.DocumentSearchRequest;
import com.marsreg.document.model.DocumentSearchResponse;
import com.marsreg.document.service.DocumentCoreService;
import com.marsreg.document.service.DocumentProcessService;
import com.marsreg.document.service.DocumentSearchService;
import com.marsreg.document.service.DocumentVectorService;
import com.marsreg.document.service.DocumentIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCoreServiceImpl implements DocumentCoreService {

    private final DocumentProcessService documentProcessService;
    private final DocumentSearchService documentSearchService;
    private final DocumentVectorService documentVectorService;
    private final DocumentIndexService documentIndexService;

    @Override
    @Transactional
    public DocumentEntity processDocument(DocumentEntity document) {
        return documentProcessService.process(document);
    }

    @Override
    public DocumentSearchResponse searchDocuments(DocumentSearchRequest request) {
        return documentSearchService.searchDocuments(request);
    }

    @Override
    @Transactional
    public void updateDocumentVector(DocumentEntity document) {
        documentVectorService.generateVector(document);
    }

    @Override
    @Transactional
    public void updateDocumentIndex(DocumentEntity document) {
        documentVectorService.updateDocumentVector(document);
    }
    
    @Override
    public String extractText(DocumentEntity document) {
        return documentProcessService.extractText(document);
    }
    
    @Override
    public String cleanText(String text) {
        return documentProcessService.cleanText(text);
    }
    
    @Override
    public String detectLanguage(String text) {
        return documentProcessService.detectLanguage(text);
    }
    
    @Override
    public List<String> smartChunkText(String text, int maxChunkSize, int overlap) {
        return documentProcessService.smartChunkText(text, maxChunkSize, overlap);
    }
} 