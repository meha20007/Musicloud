package com.musicloud.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Simple health check endpoint.
 * GET /api/health → returns "status: UP" and current time.
 * Used to verify the backend is running (by Render, Swing client, or browser).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "MusicLoud Backend",
                "timestamp", LocalDateTime.now().toString(),
                "message", "MusicLoud is running! 🎵"
        ));
    }
}
