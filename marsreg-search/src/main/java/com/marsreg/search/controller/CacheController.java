package com.marsreg.search.controller;

import com.marsreg.search.service.impl.SearchServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final SearchServiceImpl searchService;

    @PostMapping("/clear")
    public ResponseEntity<Void> clearCache() {
        searchService.clearCache();
        return ResponseEntity.ok().build();
    }
} 