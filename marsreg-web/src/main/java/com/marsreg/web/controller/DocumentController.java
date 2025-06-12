package com.marsreg.web.controller;

import com.marsreg.document.service.DocumentService;
import com.marsreg.document.dto.DocumentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getAllDocuments() {
        return ResponseEntity.ok(documentService.listDocuments(Pageable.unpaged()));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocuments(@RequestParam("files") MultipartFile[] files) {
        try {
            for (MultipartFile file : files) {
                documentService.createDocument(file, new DocumentDTO());
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("文件上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(@PathVariable Long id) {
        return documentService.getDocument(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除文档失败：" + e.getMessage());
        }
    }
} 