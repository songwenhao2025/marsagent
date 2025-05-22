package com.marsreg.document.controller;

import com.marsreg.common.exception.BusinessException;
import com.marsreg.common.response.Result;
import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentContent;
import com.marsreg.document.entity.DocumentChunkMetadata;
import com.marsreg.document.repository.DocumentContentRepository;
import com.marsreg.document.service.DocumentChunkMetadataService;
import com.marsreg.document.service.DocumentProcessService;
import com.marsreg.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "文档管理", description = "文档上传、处理和管理接口")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentContentRepository documentContentRepository;
    private final DocumentProcessService documentProcessService;
    private final DocumentChunkMetadataService metadataService;

    @Operation(summary = "上传文档")
    @PostMapping("/upload")
    public Result<Document> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(documentService.upload(file));
    }

    @Operation(summary = "获取文档信息")
    @GetMapping("/{id}")
    public Result<Document> getDocument(@PathVariable Long id) {
        return Result.success(documentService.getById(id));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }

    @Operation(summary = "获取文档内容")
    @GetMapping("/{id}/content")
    public Result<DocumentContent> getDocumentContent(@PathVariable Long id) {
        return Result.success(documentContentRepository.findByDocumentId(id)
                .orElseThrow(() -> new BusinessException("文档内容不存在")));
    }

    @Operation(summary = "获取文档分块")
    @GetMapping("/{id}/chunks")
    public Result<List<String>> getDocumentChunks(@PathVariable Long id) {
        return Result.success(documentProcessService.getChunks(id));
    }

    @Operation(summary = "分页查询文档")
    @GetMapping
    public Result<Page<Document>> list(Pageable pageable) {
        return Result.success(documentService.list(pageable));
    }

    @Operation(summary = "获取文档URL")
    @GetMapping("/{id}/url")
    public Result<String> getDocumentUrl(
            @PathVariable Long id,
            @RequestParam(defaultValue = "3600") int expirySeconds) {
        return Result.success(documentService.getDocumentUrl(id, expirySeconds));
    }

    @Operation(summary = "获取文档元数据")
    @GetMapping("/{id}/metadata")
    public Result<Map<String, Object>> getDocumentMetadata(@PathVariable Long id) {
        return Result.success(metadataService.getMetadataMapByDocumentId(id));
    }

    @Operation(summary = "获取文档分块元数据")
    @GetMapping("/{id}/chunks/{chunkId}/metadata")
    public Result<Map<String, Object>> getChunkMetadata(
            @PathVariable Long id,
            @PathVariable Long chunkId) {
        return Result.success(metadataService.getMetadataMapByChunkId(chunkId));
    }

    @Operation(summary = "更新文档分块元数据")
    @PutMapping("/{id}/chunks/{chunkId}/metadata")
    public Result<DocumentChunkMetadata> updateChunkMetadata(
            @PathVariable Long id,
            @PathVariable Long chunkId,
            @RequestBody DocumentChunkMetadata metadata) {
        metadata.setDocumentId(id);
        metadata.setChunkId(chunkId);
        return Result.success(metadataService.save(metadata));
    }

    @Operation(summary = "删除文档分块元数据")
    @DeleteMapping("/{id}/chunks/{chunkId}/metadata/{key}")
    public Result<Void> deleteChunkMetadata(
            @PathVariable Long id,
            @PathVariable Long chunkId,
            @PathVariable String key) {
        metadataService.deleteByChunkIdAndKey(chunkId, key);
        return Result.success();
    }
} 