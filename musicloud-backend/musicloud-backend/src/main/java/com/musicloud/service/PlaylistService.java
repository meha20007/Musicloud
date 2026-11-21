package com.musicloud.service;

import com.musicloud.dto.PlaylistRequest;
import com.musicloud.dto.PlaylistResponse;
import com.musicloud.dto.SongResponse;
import com.musicloud.model.PlaylistEntity;
import com.musicloud.model.SongEntity;
import com.musicloud.repository.PlaylistRepository;
import com.musicloud.repository.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Playlist management service.
 *
 * SIMPLE EXPLANATION:
 *   - Each user can only see and edit THEIR OWN playlists.
 *   - The userId from the JWT is used to enforce this isolation.
 *   - If user A tries to access user B's playlist, they get a 403 error.
 */
@Service
@Transactional(readOnly = true)
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final SongRepository songRepository;

    public PlaylistService(PlaylistRepository playlistRepository,
                           SongRepository songRepository) {
        this.playlistRepository = playlistRepository;
        this.songRepository = songRepository;
    }

    /** Get all playlists for the authenticated user */
    public List<PlaylistResponse> getUserPlaylists(String userId) {
        return playlistRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Create a new playlist for the authenticated user */
    @Transactional
    public PlaylistResponse createPlaylist(String userId, PlaylistRequest req) {
        PlaylistEntity playlist = new PlaylistEntity(req.getName().trim(), userId);
        return toResponse(playlistRepository.save(playlist));
    }

    /** Delete a playlist — only if it belongs to this user */
    @Transactional
    public void deletePlaylist(Long playlistId, String userId) {
        PlaylistEntity playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Playlist not found or does not belong to you."));
        playlistRepository.delete(playlist);
    }

    /** Add a song to a playlist — only if the playlist belongs to this user */
    @Transactional
    public PlaylistResponse addSongToPlaylist(Long playlistId, Long songId, String userId) {
        PlaylistEntity playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Playlist not found or does not belong to you."));

        SongEntity song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song not found with id: " + songId));

        // Avoid adding duplicate songs
        boolean alreadyIn = playlist.getSongs().stream()
                .anyMatch(s -> s.getId().equals(songId));
        if (!alreadyIn) {
            playlist.getSongs().add(song);
            playlistRepository.save(playlist);
        }

        return toResponse(playlist);
    }

    /** Remove a song from a playlist — only if the playlist belongs to this user */
    @Transactional
    public PlaylistResponse removeSongFromPlaylist(Long playlistId, Long songId, String userId) {
        PlaylistEntity playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Playlist not found or does not belong to you."));

        playlist.getSongs().removeIf(s -> s.getId().equals(songId));
        playlistRepository.save(playlist);
        return toResponse(playlist);
    }

    private PlaylistResponse toResponse(PlaylistEntity p) {
        List<SongResponse> songs = p.getSongs().stream()
                .map(SongResponse::from)
                .collect(Collectors.toList());
        return new PlaylistResponse(p.getId(), p.getName(), songs);
    }
}
