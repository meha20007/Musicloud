package com.musicloud.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A playlist owned by a specific user.
 * Each user can only view/modify their own playlists.
 */
@Entity
@Table(name = "ml_playlists")
public class PlaylistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** The user who owns this playlist (FK to ml_users.id) */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "ml_playlist_songs",
        joinColumns = @JoinColumn(name = "playlist_id"),
        inverseJoinColumns = @JoinColumn(name = "song_id")
    )
    private List<SongEntity> songs = new ArrayList<>();

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // ----- Constructors -----
    public PlaylistEntity() {}

    public PlaylistEntity(String name, String userId) {
        this.name = name;
        this.userId = userId;
    }

    // ----- Getters / Setters -----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<SongEntity> getSongs() { return songs; }
    public void setSongs(List<SongEntity> songs) { this.songs = songs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
