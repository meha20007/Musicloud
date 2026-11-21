package com.musicloud.dto;

/** Response DTO for a song — safe to send to Swing client */
public class SongResponse {

    private Long id;
    private String title;
    private String artist;
    private String genre;
    private Integer releaseYear;
    private String audioUrl;
    private String coverUrl;
    private Integer durationSeconds;

    public SongResponse() {}

    public SongResponse(Long id, String title, String artist, String genre,
                        Integer releaseYear, String audioUrl, String coverUrl,
                        Integer durationSeconds) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.releaseYear = releaseYear;
        this.audioUrl = audioUrl;
        this.coverUrl = coverUrl;
        this.durationSeconds = durationSeconds;
    }

    public static SongResponse from(com.musicloud.model.SongEntity e) {
        return new SongResponse(
            e.getId(), e.getTitle(), e.getArtist(), e.getGenre(),
            e.getReleaseYear(), e.getAudioUrl(), e.getCoverUrl(),
            e.getDurationSeconds()
        );
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getGenre() { return genre; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getAudioUrl() { return audioUrl; }
    public String getCoverUrl() { return coverUrl; }
    public Integer getDurationSeconds() { return durationSeconds; }
}
