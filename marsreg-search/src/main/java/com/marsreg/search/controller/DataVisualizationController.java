package com.marsreg.search.controller;

import com.marsreg.search.model.UserBehaviorStats;
import com.marsreg.search.service.DataVisualizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/visualization")
@RequiredArgsConstructor
public class DataVisualizationController {

    private final DataVisualizationService dataVisualizationService;

    @GetMapping("/search-trend")
    public ResponseEntity<Map<String, Long>> getSearchTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "DAY") DataVisualizationService.TimeInterval interval) {
        return ResponseEntity.ok(dataVisualizationService.getSearchTrend(startTime, endTime, interval));
    }

    @GetMapping("/search-type-distribution")
    public ResponseEntity<Map<String, Long>> getSearchTypeDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(dataVisualizationService.getSearchTypeDistribution(startTime, endTime));
    }

    @GetMapping("/document-type-distribution")
    public ResponseEntity<Map<String, Long>> getDocumentTypeDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(dataVisualizationService.getDocumentTypeDistribution(startTime, endTime));
    }

    @GetMapping("/user-activity")
    public ResponseEntity<Map<String, Long>> getUserActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "DAY") DataVisualizationService.TimeInterval interval) {
        return ResponseEntity.ok(dataVisualizationService.getUserActivity(startTime, endTime, interval));
    }

    @GetMapping("/user-behavior/{userId}")
    public ResponseEntity<UserBehaviorStats> getUserBehaviorAnalysis(
            @PathVariable String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(dataVisualizationService.getUserBehaviorAnalysis(userId, startTime, endTime));
    }

    @GetMapping("/performance-trend")
    public ResponseEntity<Map<String, Double>> getPerformanceTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "DAY") DataVisualizationService.TimeInterval interval) {
        return ResponseEntity.ok(dataVisualizationService.getPerformanceTrend(startTime, endTime, interval));
    }

    @GetMapping("/hot-keywords-wordcloud")
    public ResponseEntity<List<DataVisualizationService.WordCloudData>> getHotKeywordsWordCloud(
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(dataVisualizationService.getHotKeywordsWordCloud(size));
    }
} 