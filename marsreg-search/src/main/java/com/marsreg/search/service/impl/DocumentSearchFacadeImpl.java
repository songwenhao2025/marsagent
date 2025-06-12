package com.marsreg.search.service.impl;

import com.marsreg.common.model.Document;
import com.marsreg.common.dto.DocumentQueryDTO;
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
    public Page<Document> search(DocumentQueryDTO queryDTO, Pageable pageable) {
        Criteria criteria = new Criteria("title").matches(queryDTO.getKeyword())
                .or("content").matches(queryDTO.getKeyword());
        if (queryDTO.getStatus() != null) {
            criteria.and("status").is(queryDTO.getStatus());
        }
        if (queryDTO.getContentType() != null) {
            criteria.and("contentType").is(queryDTO.getContentType());
        }
        CriteriaQuery searchQuery = new CriteriaQuery(criteria);
        searchQuery.setPageable(pageable);
        
        SearchHits<DocumentIndex> hits = elasticsearchOperations.search(searchQuery, DocumentIndex.class);
        List<Document> documents = hits.getSearchHits().stream()
                .map(hit -> toDocument(hit.getContent()))
                .collect(Collectors.toList());
        return new PageImpl<>(documents, pageable, hits.getTotalHits());
    }

    private Document toDocument(DocumentIndex index) {
        return Document.builder()
                .id(Long.parseLong(index.getId()))
                .name(index.getTitle())
                .content(index.getContent())
                .type(index.getDocumentType())
                .status(index.getStatus())
                .contentType(index.getContentType())
                .customMetadata(index.getMetadata() != null ? index.getMetadata().toString() : null)
                .createTime(index.getCreateTime())
                .updateTime(index.getUpdateTime())
                .build();
    }
}