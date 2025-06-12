package com.marsreg.document.controller;

import com.marsreg.common.annotation.Log;
import com.marsreg.common.annotation.RateLimit;
import com.marsreg.common.exception.BusinessException;
import com.marsreg.common.response.Result;
import com.marsreg.document.dto.DocumentDTO;
import com.marsreg.document.entity.DocumentContent;
import com.marsreg.document.entity.DocumentChunkMetadata;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.repository.DocumentContentRepository;
import com.marsreg.document.service.DocumentChunkMetadataService;
import com.marsreg.document.service.DocumentProcessService;
import com.marsreg.document.service.DocumentService;
import com.marsreg.document.service.DocumentVectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "文档管理", description = "文档上传、处理和管理接口")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentContentRepository documentContentRepository;
    private final DocumentProcessService documentProcessService;
    private final DocumentChunkMetadataService metadataService;
    private final DocumentVectorService documentVectorService;

    @Operation(summary = "上传文档")
    @PostMapping
    @Log(module = "文档管理", operation = "上传", description = "上传文档")
    @RateLimit(key = "#request.file.originalFilename", limit = 100, time = 60)
    public ResponseEntity<DocumentDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("metadata") DocumentDTO metadata) throws IOException {
        return ResponseEntity.ok(documentService.createDocument(file, metadata));
    }

    @Operation(summary = "获取文档信息")
    @GetMapping("/{id}")
    @Log(module = "文档管理", operation = "查询", description = "查询文档详情")
    @RateLimit(limit = 200, time = 60)
    public ResponseEntity<DocumentDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocument(id).orElseThrow(() -> new BusinessException("文档不存在")));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    @Log(module = "文档管理", operation = "删除", description = "删除文档")
    @RateLimit(limit = 50, time = 60)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "获取文档内容")
    @GetMapping("/{id}/content")
    public Result<DocumentContent> getDocumentContent(@PathVariable Long id) {
        return Result.success(documentContentRepository.findByDocumentId(id)
                .orElseThrow(() -> new BusinessException("文档内容不存在")));
    }

    @Operation(summary = "分页查询文档")
    @GetMapping
    public ResponseEntity<List<DocumentDTO>> listDocuments(Pageable pageable) {
        return ResponseEntity.ok(documentService.listDocuments(pageable));
    }

    @Operation(summary = "获取文档URL")
    @GetMapping("/{id}/url")
    public String getDocumentUrl(@PathVariable Long id, @RequestParam(defaultValue = "3600") int expirySeconds) {
        return documentService.getDocumentUrl(id, expirySeconds);
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

    @Operation(summary = "按文档ID语义搜索分块")
    @GetMapping("/{documentId}/search")
    public Result<List<Map<String, Object>>> searchChunksByDocument(
            @PathVariable Long documentId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0.7") float minScore) {
        return Result.success(documentVectorService.searchChunksByDocument(documentId, query, limit, minScore));
    }

    @PostMapping("/batch-upload")
    public ResponseEntity<List<DocumentDTO>> batchUpload(@RequestParam("files") List<MultipartFile> files) {
        try {
            List<DocumentEntity> entities = documentService.batchUpload(files);
            return ResponseEntity.ok(entities.stream()
                    .map(entity -> {
                        DocumentDTO dto = new DocumentDTO();
                        BeanUtils.copyProperties(entity, dto);
                        return dto;
                    })
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            throw new BusinessException("批量文档上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDelete(@RequestBody List<Long> ids) {
        documentService.batchDelete(ids);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDTO> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentDTO metadata) {
        return ResponseEntity.ok(documentService.updateDocument(id, metadata));
    }
} 