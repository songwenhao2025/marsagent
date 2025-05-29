package com.marsreg.search.controller;

import com.marsreg.search.model.SearchSuggestion;
import com.marsreg.search.service.SearchSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search/suggestions")
@RequiredArgsConstructor
public class SearchSuggestionController {

    private final SearchSuggestionService searchSuggestionService;

    @GetMapping
    public List<SearchSuggestion> getSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "10") int size) {
        return searchSuggestionService.getSuggestions(prefix, size);
    }

    @GetMapping("/hot")
    public List<SearchSuggestion> getHotSuggestions(
            @RequestParam(defaultValue = "10") int size) {
        return searchSuggestionService.getHotSuggestions(size);
    }

    @PostMapping("/record")
    public void recordSearchKeyword(@RequestParam String keyword) {
        searchSuggestionService.recordSearchKeyword(keyword);
    }
} 