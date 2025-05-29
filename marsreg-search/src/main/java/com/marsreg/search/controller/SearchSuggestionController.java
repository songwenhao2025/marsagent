package com.marsreg.search.controller;

import com.marsreg.search.model.SearchSuggestion;
import com.marsreg.search.service.SearchSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/suggestions")
@RequiredArgsConstructor
public class SearchSuggestionController {

    private final SearchSuggestionService searchSuggestionService;

    @GetMapping
    public ResponseEntity<List<SearchSuggestion>> getSuggestions(
            @RequestParam(required = false) String prefix,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(searchSuggestionService.getSuggestions(prefix, size));
    }

    @GetMapping("/hot")
    public ResponseEntity<List<SearchSuggestion>> getHotSuggestions(
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(searchSuggestionService.getHotSuggestions(size));
    }

    @GetMapping("/personalized")
    public ResponseEntity<List<SearchSuggestion>> getPersonalizedSuggestions(
            @RequestParam String userId,
            @RequestParam(required = false) String prefix,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(searchSuggestionService.getPersonalizedSuggestions(userId, prefix, size));
    }

    @PostMapping("/record")
    public ResponseEntity<Void> recordSuggestionUsage(
            @RequestBody SearchSuggestion suggestion,
            @RequestParam(required = false) String userId) {
        searchSuggestionService.recordSuggestionUsage(suggestion, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/expand/query")
    public ResponseEntity<List<String>> expandQuery(@RequestBody String query) {
        return ResponseEntity.ok(searchSuggestionService.expandQuery(query));
    }

    @PostMapping("/expand/queries")
    public ResponseEntity<Map<String, List<String>>> expandQueries(@RequestBody List<String> queries) {
        return ResponseEntity.ok(searchSuggestionService.expandQueries(queries));
    }

    @PostMapping("/expand/term")
    public ResponseEntity<List<String>> expandTerm(@RequestBody String term) {
        return ResponseEntity.ok(searchSuggestionService.expandTerm(term));
    }

    @PostMapping("/expand/terms")
    public ResponseEntity<Map<String, List<String>>> expandTerms(@RequestBody List<String> terms) {
        return ResponseEntity.ok(searchSuggestionService.expandTerms(terms));
    }

    @GetMapping("/weight/query")
    public ResponseEntity<Double> getQueryWeight(@RequestParam String query) {
        return ResponseEntity.ok(searchSuggestionService.getQueryWeight(query));
    }

    @GetMapping("/weight/term")
    public ResponseEntity<Double> getTermWeight(@RequestParam String term) {
        return ResponseEntity.ok(searchSuggestionService.getTermWeight(term));
    }
} 