package com.musicloud.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a song in the MusicLoud cloud library.
 * Admin can add / edit / delete songs; all users can browse.
 */
@Entity
@Table(name = "ml_songs")
public class SongEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "artist", nullable = false, length = 255)
    private String artist;

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "release_year")
    private Integer releaseYear;

    /** Supabase Storage public URL for the audio file (optional) */
    @Column(name = "audio_url", length = 1000)
    private String audioUrl;

    /** Supabase Storage public URL for the album cover image (optional) */
    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    /** Duration in seconds (optional) */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // ----- Constructors -----
    public SongEntity() {}

    public SongEntity(String title, String artist, String genre, Integer releaseYear) {
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.releaseYear = releaseYear;
    }

    // ----- Getters / Setters -----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
