package com.marsreg.document.service.impl;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.service.DocumentSearchService;
import com.marsreg.search.service.DocumentSearchFacade;
import com.marsreg.document.model.DocumentSearchRequest;
import com.marsreg.document.model.DocumentSearchResponse;
import com.marsreg.document.model.MarsregDocument;
import com.marsreg.common.model.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSearchServiceImpl implements DocumentSearchService {

    private final DocumentSearchFacade documentSearchFacade;

    @Override
    public Page<DocumentEntity> search(String query, Pageable pageable) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 search(String, Pageable)");
    }

    @Override
    public Page<DocumentEntity> searchContent(String query, Pageable pageable) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 searchContent(String, Pageable)");
    }

    @Override
    public Page<DocumentEntity> searchTitle(String query, Pageable pageable) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 searchTitle(String, Pageable)");
    }

    @Override
    public Page<Map<String, Object>> searchWithHighlight(String query, Pageable pageable) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 searchWithHighlight(String, Pageable)");
    }

    @Override
    public Page<Map<String, Object>> searchContentWithHighlight(String query, Pageable pageable) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 searchContentWithHighlight(String, Pageable)");
    }

    @Override
    public Page<Map<String, Object>> searchTitleWithHighlight(String query, Pageable pageable) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 searchTitleWithHighlight(String, Pageable)");
    }

    @Override
    public List<String> getSuggestions(String prefix, int limit) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 getSuggestions(String, int)");
    }

    @Override
    public List<String> getTitleSuggestions(String prefix, int limit) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 getTitleSuggestions(String, int)");
    }

    @Override
    public List<String> getContentSuggestions(String prefix, int limit) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 getContentSuggestions(String, int)");
    }

    @Override
    public List<Map<String, Object>> vectorSearch(String query, int limit, float minScore) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 vectorSearch(String, int, float)");
    }

    @Override
    public List<Map<String, Object>> hybridSearch(String query, int limit, float minScore) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 hybridSearch(String, int, float)");
    }

    @Override
    public List<String> getPersonalizedSuggestions(String userId, String prefix, int limit) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 getPersonalizedSuggestions(String, String, int)");
    }

    @Override
    public void recordSuggestionUsage(String suggestion, String userId) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 recordSuggestionUsage(String, String)");
    }

    @Override
    public List<String> getHotSuggestions(int limit) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 getHotSuggestions(int)");
    }

    @Override
    public Page<DocumentEntity> advancedSearch(Map<String, Object> criteria, Pageable pageable) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 advancedSearch(Map, Pageable)");
    }

    @Override
    public Page<DocumentEntity> findSimilar(Long documentId, Pageable pageable) {
        throw new UnsupportedOperationException("请在 marsreg-search 模块的 DocumentSearchFacade 中实现 findSimilar(Long, Pageable)");
    }

    @Override
    public DocumentSearchResponse searchDocuments(DocumentSearchRequest request) {
        com.marsreg.common.model.DocumentSearchRequest commonRequest = com.marsreg.common.model.DocumentSearchRequest.builder()
            .query(request.getQuery())
            .page(request.getPage())
            .size(request.getSize())
            .build();

        com.marsreg.common.model.DocumentSearchResponse commonResponse = documentSearchFacade.searchDocuments(commonRequest);

        List<MarsregDocument> marsregDocuments = commonResponse.getDocuments().stream()
            .map(this::toMarsregDocument)
            .collect(Collectors.toList());

        return new DocumentSearchResponse()
            .setDocuments(marsregDocuments)
            .setTotal(commonResponse.getTotal())
            .setPage(commonResponse.getPage())
            .setSize(commonResponse.getSize());
    }

    private MarsregDocument toMarsregDocument(Document document) {
        MarsregDocument marsregDocument = new MarsregDocument();
        marsregDocument.setId(document.getId());
        marsregDocument.setTitle(document.getTitle());
        marsregDocument.setContent(document.getContent());
        marsregDocument.setType(document.getType());
        marsregDocument.setStatus(document.getStatus());
        marsregDocument.setMetadata(document.getMetadata() != null ? document.getMetadata().toString() : null);
        marsregDocument.setCreateTime(document.getCreateTime() != null ? document.getCreateTime().toString() : null);
        marsregDocument.setUpdateTime(document.getUpdateTime() != null ? document.getUpdateTime().toString() : null);
        return marsregDocument;
    }
} 