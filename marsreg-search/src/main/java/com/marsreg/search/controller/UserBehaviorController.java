package com.marsreg.search.controller;

import com.marsreg.search.model.UserBehaviorStats;
import com.marsreg.search.service.UserBehaviorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/user-behavior")
@RequiredArgsConstructor
public class UserBehaviorController {

    private final UserBehaviorService userBehaviorService;

    @GetMapping("/{userId}")
    public UserBehaviorStats getUserBehaviorStats(@PathVariable String userId) {
        return userBehaviorService.getUserBehaviorStats(userId);
    }

    @GetMapping("/{userId}/time-range")
    public UserBehaviorStats getUserBehaviorStatsByTimeRange(
            @PathVariable String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return userBehaviorService.getUserBehaviorStatsByTimeRange(userId, startTime, endTime);
    }

    @GetMapping("/{userId}/frequent-keywords")
    public List<UserBehaviorStats.KeywordStats> getUserFrequentKeywords(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int size) {
        return userBehaviorService.getUserFrequentKeywords(userId, size);
    }

    @GetMapping("/{userId}/active-time")
    public Map<String, Long> getUserActiveTimeDistribution(@PathVariable String userId) {
        return userBehaviorService.getUserActiveTimeDistribution(userId);
    }

    @GetMapping("/{userId}/recent-searches")
    public List<UserBehaviorStats.RecentSearch> getUserRecentSearches(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int size) {
        return userBehaviorService.getUserRecentSearches(userId, size);
    }
} 