package com.musicloud.service;

import com.musicloud.dto.SongRequest;
import com.musicloud.dto.SongResponse;
import com.musicloud.model.SongEntity;
import com.musicloud.repository.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for the song library.
 *
 * SIMPLE EXPLANATION:
 *   - Admin can add, edit, delete songs.
 *   - Any logged-in user (and even public) can browse and search.
 *   - Each song can have an optional cloud audio URL and cover image URL.
 */
@Service
@Transactional(readOnly = true)
public class SongService {

    private final SongRepository songRepository;

    public SongService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    /** Get all songs ordered newest first */
    public List<SongResponse> getAllSongs() {
        return songRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(SongResponse::from)
                .collect(Collectors.toList());
    }

    /** Search by title, artist, or genre */
    public List<SongResponse> searchSongs(String term) {
        if (term == null || term.isBlank()) return getAllSongs();
        return songRepository.searchSongs(term.trim())
                .stream()
                .map(SongResponse::from)
                .collect(Collectors.toList());
    }

    /** Filter by genre (case-insensitive; "All" or empty returns everything) */
    public List<SongResponse> filterByGenre(String genre) {
        if (genre == null || genre.isBlank() || genre.equalsIgnoreCase("all")) {
            return getAllSongs();
        }
        return songRepository.findByGenreIgnoreCase(genre.trim())
                .stream()
                .map(SongResponse::from)
                .collect(Collectors.toList());
    }

    /** Get single song by ID */
    public SongResponse getSongById(Long id) {
        SongEntity song = songRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Song not found with id: " + id));
        return SongResponse.from(song);
    }

    /** Admin: Add a new song */
    @Transactional
    public SongResponse addSong(SongRequest req) {
        SongEntity song = new SongEntity(
                req.getTitle().trim(),
                req.getArtist().trim(),
                req.getGenre().trim(),
                req.getReleaseYear()
        );
        if (req.getAudioUrl() != null) song.setAudioUrl(req.getAudioUrl().trim());
        if (req.getCoverUrl() != null) song.setCoverUrl(req.getCoverUrl().trim());

        return SongResponse.from(songRepository.save(song));
    }

    /** Admin: Edit an existing song */
    @Transactional
    public SongResponse updateSong(Long id, SongRequest req) {
        SongEntity song = songRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Song not found with id: " + id));

        song.setTitle(req.getTitle().trim());
        song.setArtist(req.getArtist().trim());
        song.setGenre(req.getGenre().trim());
        song.setReleaseYear(req.getReleaseYear());
        if (req.getAudioUrl() != null) song.setAudioUrl(req.getAudioUrl().trim());
        if (req.getCoverUrl() != null) song.setCoverUrl(req.getCoverUrl().trim());

        return SongResponse.from(songRepository.save(song));
    }

    /** Admin: Delete a song */
    @Transactional
    public void deleteSong(Long id) {
        if (!songRepository.existsById(id)) {
            throw new IllegalArgumentException("Song not found with id: " + id);
        }
        songRepository.deleteById(id);
    }
}
