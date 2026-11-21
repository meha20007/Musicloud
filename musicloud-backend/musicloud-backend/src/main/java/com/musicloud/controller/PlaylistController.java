package com.musicloud.controller;

import com.musicloud.dto.PlaylistRequest;
import com.musicloud.dto.PlaylistResponse;
import com.musicloud.security.MusicLoudPrincipal;
import com.musicloud.service.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Playlist REST API — USER-SCOPED (each user sees only their own playlists).
 *
 * GET    /api/playlists               → List my playlists
 * POST   /api/playlists               → Create a new playlist
 * DELETE /api/playlists/{id}          → Delete my playlist
 * POST   /api/playlists/{id}/songs/{songId}    → Add song to my playlist
 * DELETE /api/playlists/{id}/songs/{songId}    → Remove song from my playlist
 */
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping
    public ResponseEntity<List<PlaylistResponse>> getMyPlaylists(Authentication auth) {
        String userId = getPrincipal(auth).getUserId();
        return ResponseEntity.ok(playlistService.getUserPlaylists(userId));
    }

    @PostMapping
    public ResponseEntity<?> createPlaylist(@Valid @RequestBody PlaylistRequest req,
                                            Authentication auth) {
        try {
            String userId = getPrincipal(auth).getUserId();
            PlaylistResponse created = playlistService.createPlaylist(userId, req);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlaylist(@PathVariable Long id, Authentication auth) {
        try {
            playlistService.deletePlaylist(id, getPrincipal(auth).getUserId());
            return ResponseEntity.ok(Map.of("message", "Playlist deleted."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/songs/{songId}")
    public ResponseEntity<?> addSong(@PathVariable Long id,
                                     @PathVariable Long songId,
                                     Authentication auth) {
        try {
            return ResponseEntity.ok(
                    playlistService.addSongToPlaylist(id, songId, getPrincipal(auth).getUserId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<?> removeSong(@PathVariable Long id,
                                        @PathVariable Long songId,
                                        Authentication auth) {
        try {
            return ResponseEntity.ok(
                    playlistService.removeSongFromPlaylist(id, songId, getPrincipal(auth).getUserId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    private MusicLoudPrincipal getPrincipal(Authentication auth) {
        return (MusicLoudPrincipal) auth.getPrincipal();
    }
}
