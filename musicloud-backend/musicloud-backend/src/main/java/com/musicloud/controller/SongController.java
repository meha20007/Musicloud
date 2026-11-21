package com.musicloud.controller;

import com.musicloud.dto.SongRequest;
import com.musicloud.dto.SongResponse;
import com.musicloud.service.SongService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Song Library REST API.
 *
 * GET  /api/songs            → Browse all songs (public)
 * GET  /api/songs/search     → Search by title/artist/genre (public)
 * GET  /api/songs/filter     → Filter by genre (public)
 * GET  /api/songs/{id}       → Get single song (public)
 * POST /api/songs            → Add song (Admin only)
 * PUT  /api/songs/{id}       → Edit song (Admin only)
 * DELETE /api/songs/{id}     → Delete song (Admin only)
 */
@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping
    public ResponseEntity<List<SongResponse>> getAllSongs() {
        return ResponseEntity.ok(songService.getAllSongs());
    }

    @GetMapping("/search")
    public ResponseEntity<List<SongResponse>> searchSongs(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(songService.searchSongs(q));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<SongResponse>> filterByGenre(
            @RequestParam(defaultValue = "all") String genre) {
        return ResponseEntity.ok(songService.filterByGenre(genre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSongById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(songService.getSongById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addSong(@Valid @RequestBody SongRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(songService.addSong(request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSong(@PathVariable Long id,
                                        @Valid @RequestBody SongRequest request) {
        try {
            return ResponseEntity.ok(songService.updateSong(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSong(@PathVariable Long id) {
        try {
            songService.deleteSong(id);
            return ResponseEntity.ok(Map.of("message", "Song deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
