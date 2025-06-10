package com.marsreg.search.service;

import com.marsreg.common.model.Document;
import com.marsreg.common.model.DocumentSearchRequest;
import com.marsreg.common.model.DocumentSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;

public interface DocumentSearchFacade {
    Page<Document> search(String query, Pageable pageable);
    Page<Map<String, Object>> searchWithHighlight(String query, Pageable pageable);
    DocumentSearchResponse searchDocuments(DocumentSearchRequest request);
} 