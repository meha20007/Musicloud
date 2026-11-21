package com.musicloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PlaylistRequest {

    @NotBlank(message = "Playlist name is required")
    @Size(min = 1, max = 100, message = "Name must be 1–100 characters")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
