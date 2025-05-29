package com.marsreg.search.controller;

import com.marsreg.search.model.SearchStatistics;
import com.marsreg.search.service.SearchStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/statistics")
@RequiredArgsConstructor
public class SearchStatisticsController {

    private final SearchStatisticsService searchStatisticsService;

    @GetMapping
    public SearchStatistics getOverallStatistics() {
        return searchStatisticsService.getOverallStatistics();
    }

    @GetMapping("/time-range")
    public SearchStatistics getStatisticsByTimeRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return searchStatisticsService.getStatisticsByTimeRange(startTime, endTime);
    }

    @GetMapping("/hot-keywords")
    public List<SearchStatistics.KeywordStats> getHotKeywords(
            @RequestParam(defaultValue = "10") int size) {
        return searchStatisticsService.getHotKeywords(size);
    }

    @GetMapping("/performance")
    public Map<String, Double> getPerformanceMetrics() {
        return searchStatisticsService.getPerformanceMetrics();
    }
} 