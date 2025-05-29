package com.marsreg.search.controller;

import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResult;
import com.marsreg.search.model.SearchType;
import com.marsreg.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    public List<SearchResult> search(@RequestBody SearchRequest request) {
        return searchService.search(request);
    }

    @GetMapping("/vector")
    public List<SearchResult> vectorSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0.5") float minSimilarity,
            @RequestParam(required = false) List<String> documentTypes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) List<String> sortFields,
            @RequestParam(required = false) List<String> sortOrders) {
        
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .searchType(SearchType.VECTOR)
            .size(size)
            .minSimilarity(minSimilarity)
            .documentTypes(documentTypes)
            .startTime(startTime)
            .endTime(endTime)
            .tags(tags)
            .build();
        
        // 添加排序字段
        if (sortFields != null && !sortFields.isEmpty()) {
            request.setSortFields(sortFields.stream()
                .map((field, index) -> SearchRequest.SortField.builder()
                    .field(field)
                    .order(sortOrders != null && index < sortOrders.size() && 
                        "asc".equalsIgnoreCase(sortOrders.get(index)) ? 
                        SearchRequest.SortField.SortOrder.ASC : 
                        SearchRequest.SortField.SortOrder.DESC)
                    .build())
                .collect(Collectors.toList()));
        }
        
        return searchService.search(request);
    }

    @GetMapping("/keyword")
    public List<SearchResult> keywordSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<String> documentTypes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) List<String> sortFields,
            @RequestParam(required = false) List<String> sortOrders) {
        
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .searchType(SearchType.KEYWORD)
            .size(size)
            .documentTypes(documentTypes)
            .startTime(startTime)
            .endTime(endTime)
            .tags(tags)
            .build();
        
        // 添加排序字段
        if (sortFields != null && !sortFields.isEmpty()) {
            request.setSortFields(sortFields.stream()
                .map((field, index) -> SearchRequest.SortField.builder()
                    .field(field)
                    .order(sortOrders != null && index < sortOrders.size() && 
                        "asc".equalsIgnoreCase(sortOrders.get(index)) ? 
                        SearchRequest.SortField.SortOrder.ASC : 
                        SearchRequest.SortField.SortOrder.DESC)
                    .build())
                .collect(Collectors.toList()));
        }
        
        return searchService.search(request);
    }

    @GetMapping("/hybrid")
    public List<SearchResult> hybridSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0.5") float minSimilarity,
            @RequestParam(required = false) List<String> documentTypes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) List<String> sortFields,
            @RequestParam(required = false) List<String> sortOrders) {
        
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .searchType(SearchType.HYBRID)
            .size(size)
            .minSimilarity(minSimilarity)
            .documentTypes(documentTypes)
            .startTime(startTime)
            .endTime(endTime)
            .tags(tags)
            .build();
        
        // 添加排序字段
        if (sortFields != null && !sortFields.isEmpty()) {
            request.setSortFields(sortFields.stream()
                .map((field, index) -> SearchRequest.SortField.builder()
                    .field(field)
                    .order(sortOrders != null && index < sortOrders.size() && 
                        "asc".equalsIgnoreCase(sortOrders.get(index)) ? 
                        SearchRequest.SortField.SortOrder.ASC : 
                        SearchRequest.SortField.SortOrder.DESC)
                    .build())
                .collect(Collectors.toList()));
        }
        
        return searchService.search(request);
    }
} 