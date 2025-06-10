package com.marsreg.document.controller;

import com.marsreg.common.response.Result;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.service.DocumentSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "文档搜索", description = "文档搜索接口")
@RestController
@RequestMapping("/api/documents/search")
@RequiredArgsConstructor
public class DocumentSearchController {

    private final DocumentSearchService documentSearchService;

    @Operation(summary = "搜索文档")
    @GetMapping
    public ResponseEntity<Page<DocumentEntity>> search(@RequestParam String keyword, Pageable pageable) {
        return ResponseEntity.ok(documentSearchService.search(keyword, pageable));
    }

    @Operation(summary = "搜索文档内容")
    @GetMapping("/content")
    public Result<Page<DocumentEntity>> searchContent(
            @RequestParam String query,
            Pageable pageable) {
        return Result.success(documentSearchService.searchContent(query, pageable));
    }

    @Operation(summary = "搜索文档标题")
    @GetMapping("/title")
    public Result<Page<DocumentEntity>> searchTitle(
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

    @Operation(summary = "获取搜索建议")
    @GetMapping("/suggestions")
    public Result<List<String>> getSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(documentSearchService.getSuggestions(prefix, limit));
    }

    @Operation(summary = "获取标题搜索建议")
    @GetMapping("/title/suggestions")
    public Result<List<String>> getTitleSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(documentSearchService.getTitleSuggestions(prefix, limit));
    }

    @Operation(summary = "获取内容搜索建议")
    @GetMapping("/content/suggestions")
    public Result<List<String>> getContentSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(documentSearchService.getContentSuggestions(prefix, limit));
    }

    @PostMapping("/advanced")
    public ResponseEntity<Page<DocumentEntity>> advancedSearch(@RequestBody Map<String, Object> criteria, Pageable pageable) {
        return ResponseEntity.ok(documentSearchService.advancedSearch(criteria, pageable));
    }

    @GetMapping("/similar/{documentId}")
    public ResponseEntity<Page<DocumentEntity>> findSimilar(@PathVariable Long documentId, Pageable pageable) {
        return ResponseEntity.ok(documentSearchService.findSimilar(documentId, pageable));
    }
} 