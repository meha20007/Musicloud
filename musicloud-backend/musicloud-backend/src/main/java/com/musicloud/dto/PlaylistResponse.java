package com.musicloud.dto;

import java.util.List;

/** Response DTO for a user playlist */
public class PlaylistResponse {

    private Long id;
    private String name;
    private List<SongResponse> songs;

    public PlaylistResponse() {}

    public PlaylistResponse(Long id, String name, List<SongResponse> songs) {
        this.id = id;
        this.name = name;
        this.songs = songs;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public List<SongResponse> getSongs() { return songs; }
}
