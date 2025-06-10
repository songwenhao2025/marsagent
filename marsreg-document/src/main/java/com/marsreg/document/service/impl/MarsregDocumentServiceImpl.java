package com.marsreg.document.service.impl;

import com.marsreg.document.model.DocumentSearchRequest;
import com.marsreg.document.model.DocumentSearchResponse;
import com.marsreg.document.model.MarsregDocument;
import com.marsreg.document.repository.MarsregDocumentRepository;
import com.marsreg.document.service.MarsregDocumentService;
import com.marsreg.search.service.DocumentSearchFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarsregDocumentServiceImpl implements MarsregDocumentService {

    private final MarsregDocumentRepository documentRepository;
    private final DocumentSearchFacade documentSearchFacade;

    @Override
    public MarsregDocument createDocument(MarsregDocument document) {
        return documentRepository.save(document);
    }

    @Override
    public MarsregDocument getDocument(String id) {
        return documentRepository.findById(id).orElse(null);
    }

    @Override
    public MarsregDocument updateDocument(String id, MarsregDocument document) {
        if (!documentRepository.existsById(id)) {
            return null;
        }
        document.setId(id);
        return documentRepository.save(document);
    }

    @Override
    public boolean deleteDocument(String id) {
        if (!documentRepository.existsById(id)) {
            return false;
        }
        documentRepository.deleteById(id);
        return true;
    }

    @Override
    public DocumentSearchResponse searchDocuments(DocumentSearchRequest request) {
        com.marsreg.common.model.DocumentSearchRequest commonRequest = com.marsreg.common.model.DocumentSearchRequest.builder()
            .query(request.getQuery())
            .page(request.getPage())
            .size(request.getSize())
            .build();
            
        com.marsreg.common.model.DocumentSearchResponse commonResponse = documentSearchFacade.searchDocuments(commonRequest);
        
        DocumentSearchResponse response = new DocumentSearchResponse();
        response.setDocuments(commonResponse.getDocuments().stream()
            .map(doc -> {
                MarsregDocument marsregDoc = new MarsregDocument();
                marsregDoc.setId(doc.getId().toString());
                marsregDoc.setTitle(doc.getTitle());
                marsregDoc.setContent(doc.getContent());
                marsregDoc.setType(doc.getType());
                marsregDoc.setStatus(doc.getStatus());
                return marsregDoc;
            })
            .collect(Collectors.toList()));
        response.setTotal(commonResponse.getTotal());
        response.setPage(commonResponse.getPage());
        response.setSize(commonResponse.getSize());
        return response;
    }
} 