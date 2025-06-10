package com.marsreg.search.service.impl;

import com.marsreg.common.model.Document;
import com.marsreg.common.model.DocumentSearchRequest;
import com.marsreg.common.model.DocumentSearchResponse;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.DocumentSearchFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentSearchFacadeImpl implements DocumentSearchFacade {
    @Autowired
    private DocumentIndexRepository documentIndexRepository;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<Document> search(String query, Pageable pageable) {
        Criteria criteria = new Criteria("title").matches(query)
                .or("content").matches(query);
        CriteriaQuery searchQuery = new CriteriaQuery(criteria);
        searchQuery.setPageable(pageable);
        
        SearchHits<DocumentIndex> hits = elasticsearchOperations.search(searchQuery, DocumentIndex.class);
        List<Document> documents = hits.getSearchHits().stream()
                .map(hit -> toDocument(hit.getContent()))
                .collect(Collectors.toList());
        return new PageImpl<>(documents, pageable, hits.getTotalHits());
    }

    @Override
    public Page<Map<String, Object>> searchWithHighlight(String query, Pageable pageable) {
        HighlightFieldParameters fieldParams = HighlightFieldParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .withFragmentSize(150)
                .withNumberOfFragments(3)
                .build();
        HighlightField titleField = new HighlightField("title", fieldParams);
        HighlightField contentField = new HighlightField("content", fieldParams);
        Highlight highlight = new Highlight(List.of(titleField, contentField));

        Criteria criteria = new Criteria("title").matches(query)
                .or("content").matches(query);
        CriteriaQuery searchQuery = new CriteriaQuery(criteria);
        searchQuery.setPageable(pageable);
        searchQuery.setHighlightQuery(new HighlightQuery(highlight, DocumentIndex.class));

        SearchHits<DocumentIndex> hits = elasticsearchOperations.search(searchQuery, DocumentIndex.class);
        List<Map<String, Object>> result = hits.getSearchHits().stream().map(hit -> {
            Document document = toDocument(hit.getContent());
            Map<String, List<String>> highlights = hit.getHighlightFields();
            return Map.of(
                    "document", document,
                    "highlights", highlights
            );
        }).collect(Collectors.toList());
        return new PageImpl<>(result, pageable, hits.getTotalHits());
    }

    @Override
    public DocumentSearchResponse searchDocuments(DocumentSearchRequest request) {
        CriteriaQuery searchQuery = buildSearchQuery(request);
        SearchHits<DocumentIndex> hits = elasticsearchOperations.search(searchQuery, DocumentIndex.class);
        
        List<Document> documents = hits.getSearchHits().stream()
                .map(hit -> toDocument(hit.getContent()))
                .collect(Collectors.toList());
        
        return DocumentSearchResponse.builder()
                .documents(documents)
                .total(hits.getTotalHits())
                .page(request.getPage())
                .size(request.getSize())
                .totalPages((int) Math.ceil((double) hits.getTotalHits() / request.getSize()))
                .hasNext(request.getPage() < (int) Math.ceil((double) hits.getTotalHits() / request.getSize()) - 1)
                .hasPrevious(request.getPage() > 0)
                .highlights(extractHighlights(hits))
                .build();
    }

    private Document toDocument(DocumentIndex index) {
        return Document.builder()
                .id(index.getDocumentId())
                .title(index.getTitle())
                .content(index.getContent())
                .type(index.getDocumentType())
                .status(index.getStatus())
                .contentType(index.getContentType())
                .originalName(index.getOriginalName())
                .size(index.getSize())
                .createTime(index.getCreateTime())
                .updateTime(index.getUpdateTime())
                .createBy(index.getCreateBy())
                .updateBy(index.getUpdateBy())
                .metadata(index.getMetadata())
                .build();
    }

    private CriteriaQuery buildSearchQuery(DocumentSearchRequest request) {
        Criteria criteria = new Criteria("title").matches(request.getQuery())
                .or("content").matches(request.getQuery());
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(Pageable.ofSize(request.getSize()).withPage(request.getPage()));

        if (request.isHighlight()) {
            HighlightFieldParameters fieldParams = HighlightFieldParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>")
                    .withFragmentSize(150)
                    .withNumberOfFragments(3)
                    .build();
            
            List<HighlightField> highlightFields = request.getHighlightFields().stream()
                    .map(field -> new HighlightField(field, fieldParams))
                    .collect(Collectors.toList());
            
            Highlight highlight = new Highlight(highlightFields);
            query.setHighlightQuery(new HighlightQuery(highlight, DocumentIndex.class));
        }

        return query;
    }

    private Map<String, List<String>> extractHighlights(SearchHits<DocumentIndex> hits) {
        return hits.getSearchHits().stream()
                .collect(Collectors.toMap(
                        hit -> hit.getContent().getDocumentId(),
                        hit -> hit.getHighlightFields().values().stream()
                                .flatMap(List::stream)
                                .collect(Collectors.toList())
                ));
    }
} 