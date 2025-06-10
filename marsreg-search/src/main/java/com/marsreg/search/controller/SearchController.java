package com.marsreg.search.controller;

import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResponse;
import com.marsreg.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "搜索服务", description = "提供文档搜索功能")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "执行搜索", description = "支持关键词搜索、向量搜索和混合搜索")
    @PostMapping
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return searchService.search(request);
    }

    @Operation(summary = "关键词搜索", description = "使用关键词进行全文搜索")
    @PostMapping("/keyword")
    public SearchResponse keywordSearch(@Valid @RequestBody SearchRequest request) {
        return searchService.keywordSearch(request);
    }

    @Operation(summary = "向量搜索", description = "使用向量进行语义搜索")
    @PostMapping("/vector")
    public SearchResponse vectorSearch(@Valid @RequestBody SearchRequest request) {
        return searchService.vectorSearch(request);
    }

    @Operation(summary = "混合搜索", description = "结合关键词和向量进行混合搜索")
    @PostMapping("/hybrid")
    public SearchResponse hybridSearch(@Valid @RequestBody SearchRequest request) {
        return searchService.hybridSearch(request);
    }
} 