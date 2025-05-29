package com.marsreg.search.controller;

import com.marsreg.search.service.SearchExpansionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/expansion")
@RequiredArgsConstructor
public class SearchExpansionController {

    private final SearchExpansionService searchExpansionService;

    @GetMapping("/query/{query}")
    public ResponseEntity<List<String>> expandQuery(@PathVariable String query) {
        return ResponseEntity.ok(searchExpansionService.expandQuery(query));
    }

    @PostMapping("/queries")
    public ResponseEntity<Map<String, List<String>>> expandQueries(@RequestBody List<String> queries) {
        return ResponseEntity.ok(searchExpansionService.expandQueries(queries));
    }

    @GetMapping("/term/{term}")
    public ResponseEntity<List<String>> expandTerm(@PathVariable String term) {
        return ResponseEntity.ok(searchExpansionService.expandTerm(term));
    }

    @PostMapping("/terms")
    public ResponseEntity<Map<String, List<String>>> expandTerms(@RequestBody List<String> terms) {
        return ResponseEntity.ok(searchExpansionService.expandTerms(terms));
    }

    @GetMapping("/query/{query}/weight")
    public ResponseEntity<Double> getQueryWeight(@PathVariable String query) {
        return ResponseEntity.ok(searchExpansionService.getQueryWeight(query));
    }

    @GetMapping("/term/{term}/weight")
    public ResponseEntity<Double> getTermWeight(@PathVariable String term) {
        return ResponseEntity.ok(searchExpansionService.getTermWeight(term));
    }
} 