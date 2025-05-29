package com.marsreg.search.controller;

import com.marsreg.search.model.SearchStatistics;
import com.marsreg.search.model.UserBehaviorStats;
import com.marsreg.search.service.SearchStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/statistics")
@RequiredArgsConstructor
public class SearchStatisticsController {

    private final SearchStatisticsService searchStatisticsService;

    @GetMapping("/overall")
    public ResponseEntity<SearchStatistics> getOverallStatistics() {
        return ResponseEntity.ok(searchStatisticsService.getOverallStatistics());
    }

    @GetMapping("/time-range")
    public ResponseEntity<SearchStatistics> getStatisticsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(searchStatisticsService.getStatisticsByTimeRange(startTime, endTime));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserBehaviorStats> getUserBehaviorStats(@PathVariable String userId) {
        return ResponseEntity.ok(searchStatisticsService.getUserBehaviorStats(userId));
    }

    @GetMapping("/user/{userId}/time-range")
    public ResponseEntity<UserBehaviorStats> getUserBehaviorStatsByTimeRange(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(searchStatisticsService.getUserBehaviorStatsByTimeRange(userId, startTime, endTime));
    }

    @GetMapping("/hot-keywords")
    public ResponseEntity<List<SearchStatistics.KeywordStats>> getHotKeywords(
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(searchStatisticsService.getHotKeywords(size));
    }

    @GetMapping("/user/{userId}/frequent-keywords")
    public ResponseEntity<List<UserBehaviorStats.KeywordStats>> getUserFrequentKeywords(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(searchStatisticsService.getUserFrequentKeywords(userId, size));
    }

    @GetMapping("/user/{userId}/active-time")
    public ResponseEntity<Map<String, Long>> getUserActiveTimeDistribution(@PathVariable String userId) {
        return ResponseEntity.ok(searchStatisticsService.getUserActiveTimeDistribution(userId));
    }

    @GetMapping("/user/{userId}/recent-searches")
    public ResponseEntity<List<UserBehaviorStats.RecentSearch>> getUserRecentSearches(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(searchStatisticsService.getUserRecentSearches(userId, size));
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<String, Double>> getPerformanceMetrics() {
        return ResponseEntity.ok(searchStatisticsService.getPerformanceMetrics());
    }
} 