package com.marsreg.document.service;

import com.marsreg.document.model.DocumentSearchRequest;
import com.marsreg.document.model.DocumentSearchResponse;
import com.marsreg.document.model.MarsregDocument;

public interface MarsregDocumentService {
    MarsregDocument createDocument(MarsregDocument document);
    MarsregDocument getDocument(String id);
    MarsregDocument updateDocument(String id, MarsregDocument document);
    boolean deleteDocument(String id);
    DocumentSearchResponse searchDocuments(DocumentSearchRequest request);
} 