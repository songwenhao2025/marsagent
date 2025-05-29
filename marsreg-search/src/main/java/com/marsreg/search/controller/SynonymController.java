package com.marsreg.search.controller;

import com.marsreg.search.model.SynonymGroup;
import com.marsreg.search.service.SynonymService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/synonyms")
@RequiredArgsConstructor
public class SynonymController {

    private final SynonymService synonymService;

    @GetMapping("/term/{term}")
    public ResponseEntity<List<String>> getSynonyms(@PathVariable String term) {
        return ResponseEntity.ok(synonymService.getSynonyms(term));
    }

    @PostMapping("/terms")
    public ResponseEntity<Map<String, List<String>>> getSynonymsForTerms(@RequestBody List<String> terms) {
        return ResponseEntity.ok(synonymService.getSynonymsForTerms(terms));
    }

    @PostMapping("/group")
    public ResponseEntity<String> addSynonymGroup(@RequestBody SynonymGroup group) {
        String groupId = synonymService.addSynonymGroup(group);
        return ResponseEntity.ok(groupId);
    }

    @PutMapping("/group")
    public ResponseEntity<Void> updateSynonymGroup(@RequestBody SynonymGroup group) {
        synonymService.updateSynonymGroup(group);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<Void> deleteSynonymGroup(@PathVariable String groupId) {
        synonymService.deleteSynonymGroup(groupId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<SynonymGroup> getSynonymGroup(@PathVariable String groupId) {
        SynonymGroup group = synonymService.getSynonymGroup(groupId);
        return group != null ? ResponseEntity.ok(group) : ResponseEntity.notFound().build();
    }

    @GetMapping("/groups")
    public ResponseEntity<List<SynonymGroup>> getAllSynonymGroups() {
        return ResponseEntity.ok(synonymService.getAllSynonymGroups());
    }

    @GetMapping("/groups/category/{category}")
    public ResponseEntity<List<SynonymGroup>> getSynonymGroupsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(synonymService.getSynonymGroupsByCategory(category));
    }

    @PostMapping("/reload")
    public ResponseEntity<Void> reloadSynonyms() {
        synonymService.reloadSynonyms();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/groups")
    public ResponseEntity<Void> addSynonymGroups(@RequestBody List<SynonymGroup> groups) {
        synonymService.addSynonymGroups(groups);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/groups")
    public ResponseEntity<Void> deleteSynonymGroups(@RequestBody List<String> groupIds) {
        synonymService.deleteSynonymGroups(groupIds);
        return ResponseEntity.ok().build();
    }
} 