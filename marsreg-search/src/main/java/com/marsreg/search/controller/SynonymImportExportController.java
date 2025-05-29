package com.marsreg.search.controller;

import com.marsreg.search.model.SynonymGroup;
import com.marsreg.search.service.SynonymImportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/synonyms/import-export")
@RequiredArgsConstructor
public class SynonymImportExportController {

    private final SynonymImportExportService importExportService;

    @GetMapping("/formats")
    public ResponseEntity<List<String>> getSupportedFormats() {
        return ResponseEntity.ok(importExportService.getSupportedFormats());
    }

    @PostMapping("/export")
    public ResponseEntity<Resource> exportSynonyms(
            @RequestParam String format,
            @RequestParam(required = false) String category) {
        try {
            String filePath = importExportService.exportSynonyms(format, category);
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(importExportService.getMediaType(format)))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/import")
    public ResponseEntity<Integer> importSynonyms(
            @RequestParam("file") MultipartFile file,
            @RequestParam String format) {
        try {
            // 保存上传的文件
            Path tempFile = Files.createTempFile("synonym_import_", "." + format.toLowerCase());
            file.transferTo(tempFile);

            // 导入同义词
            int count = importExportService.importSynonyms(tempFile.toString(), format);

            // 删除临时文件
            Files.delete(tempFile);

            return ResponseEntity.ok(count);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/export/string")
    public ResponseEntity<String> exportSynonymsToString(
            @RequestBody List<SynonymGroup> groups,
            @RequestParam String format) {
        try {
            String content = importExportService.exportSynonymsToString(groups, format);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(importExportService.getMediaType(format)))
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/import/string")
    public ResponseEntity<List<SynonymGroup>> importSynonymsFromString(
            @RequestBody String content,
            @RequestParam String format) {
        try {
            List<SynonymGroup> groups = importExportService.importSynonymsFromString(content, format);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 