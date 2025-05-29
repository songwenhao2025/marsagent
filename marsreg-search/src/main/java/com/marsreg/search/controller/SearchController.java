package com.marsreg.search.controller;

import com.marsreg.search.model.*;
import com.marsreg.search.service.SearchService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;
    
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    
    @PostMapping("/vector")
    public SearchResponse vectorSearch(@RequestBody SearchRequest request) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(request.getQuery());
        searchRequest.setSearchType(SearchType.VECTOR);
        searchRequest.setSize(request.getSize());
        searchRequest.setMinSimilarity(request.getMinSimilarity());
        searchRequest.setFilter(request.getFilter());
        searchRequest.setSortFields(request.getSortFields().stream()
            .map(field -> {
                SearchRequest.SortField sortField = new SearchRequest.SortField();
                sortField.setField(field.getField());
                sortField.setOrder(field.getOrder());
                return sortField;
            })
            .collect(Collectors.toList()));
        return searchService.search(searchRequest);
    }
    
    @PostMapping("/keyword")
    public SearchResponse keywordSearch(@RequestBody SearchRequest request) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(request.getQuery());
        searchRequest.setSearchType(SearchType.KEYWORD);
        searchRequest.setSize(request.getSize());
        searchRequest.setFilter(request.getFilter());
        searchRequest.setSortFields(request.getSortFields().stream()
            .map(field -> {
                SearchRequest.SortField sortField = new SearchRequest.SortField();
                sortField.setField(field.getField());
                sortField.setOrder(field.getOrder());
                return sortField;
            })
            .collect(Collectors.toList()));
        return searchService.search(searchRequest);
    }
    
    @PostMapping("/hybrid")
    public SearchResponse hybridSearch(@RequestBody SearchRequest request) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(request.getQuery());
        searchRequest.setSearchType(SearchType.HYBRID);
        searchRequest.setSize(request.getSize());
        searchRequest.setMinSimilarity(request.getMinSimilarity());
        searchRequest.setFilter(request.getFilter());
        searchRequest.setSortFields(request.getSortFields().stream()
            .map(field -> {
                SearchRequest.SortField sortField = new SearchRequest.SortField();
                sortField.setField(field.getField());
                sortField.setOrder(field.getOrder());
                return sortField;
            })
            .collect(Collectors.toList()));
        return searchService.search(searchRequest);
    }
} 