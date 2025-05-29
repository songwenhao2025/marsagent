package com.marsreg.cache.controller;

import com.marsreg.cache.service.CacheNotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache/notify")
@RequiredArgsConstructor
public class CacheNotifyController {

    private final CacheNotifyService notifyService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> notify(
            @RequestParam String type,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "INFO") CacheNotifyService.NotifyLevel level) {
        
        notifyService.notify(type, title, content, level);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification sent successfully");
        return ResponseEntity.ok(response);
    }
} 