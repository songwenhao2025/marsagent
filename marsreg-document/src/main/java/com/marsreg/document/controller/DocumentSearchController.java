package com.marsreg.document.controller;

import com.marsreg.common.response.Result;
import com.marsreg.document.entity.Document;
import com.marsreg.document.service.DocumentSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "文档搜索", description = "文档搜索接口")
@RestController
@RequestMapping("/api/documents/search")
@RequiredArgsConstructor
public class DocumentSearchController {

    private final DocumentSearchService documentSearchService;

    @Operation(summary = "搜索文档")
    @GetMapping
    public Result<Page<Document>> search(
            @RequestParam String query,
            Pageable pageable) {
        return Result.success(documentSearchService.search(query, pageable));
    }

    @Operation(summary = "搜索文档内容")
    @GetMapping("/content")
    public Result<Page<Document>> searchContent(
            @RequestParam String query,
            Pageable pageable) {
        return Result.success(documentSearchService.searchContent(query, pageable));
    }

    @Operation(summary = "搜索文档标题")
    @GetMapping("/title")
    public Result<Page<Document>> searchTitle(
            @RequestParam String query,
            Pageable pageable) {
        return Result.success(documentSearchService.searchTitle(query, pageable));
    }

    @Operation(summary = "搜索文档并高亮")
    @GetMapping("/highlight")
    public Result<Page<Map<String, Object>>> searchWithHighlight(
            @RequestParam String query,
            Pageable pageable) {
        return Result.success(documentSearchService.searchWithHighlight(query, pageable));
    }

    @Operation(summary = "搜索文档内容并高亮")
    @GetMapping("/content/highlight")
    public Result<Page<Map<String, Object>>> searchContentWithHighlight(
            @RequestParam String query,
            Pageable pageable) {
        return Result.success(documentSearchService.searchContentWithHighlight(query, pageable));
    }

    @Operation(summary = "搜索文档标题并高亮")
    @GetMapping("/title/highlight")
    public Result<Page<Map<String, Object>>> searchTitleWithHighlight(
            @RequestParam String query,
            Pageable pageable) {
        return Result.success(documentSearchService.searchTitleWithHighlight(query, pageable));
    }
} 