package cloud;

import cloud.ApiClient.ApiResponse;
import code.SongsData;
import code.SongsFileHandling;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Asynchronous song API access with local-file fallback while the backend is offline. */
public class CloudSongService {

    public static void loadAllAsync(BiConsumer<ArrayList<SongsData>, String> callback) {
        CompletableFuture.runAsync(() -> {
            if (ApiClient.isCloudAvailable()) {
                ApiResponse response = ApiClient.get("/api/songs");
                if (response.isSuccess()) {
                    try {
                        callback.accept(parseJsonSongArray(response.getBody()), null);
                        return;
                    } catch (IllegalArgumentException ex) {
                        callback.accept(localSongs(), "The server returned an invalid song list.");
                        return;
                    }
                }
                if (!response.isNetworkError()) {
                    callback.accept(localSongs(), errorMessage(response, "Could not load songs from the server."));
                    return;
                }
            }
            callback.accept(localSongs(), null);
        });
    }

    public static void searchAsync(String term, BiConsumer<ArrayList<SongsData>, String> callback) {
        CompletableFuture.runAsync(() -> {
            if (ApiClient.isCloudAvailable()) {
                String path = "/api/songs/search?q=" + URLEncoder.encode(term, StandardCharsets.UTF_8);
                ApiResponse response = ApiClient.get(path);
                if (response.isSuccess()) {
                    try {
                        callback.accept(parseJsonSongArray(response.getBody()), null);
                        return;
                    } catch (IllegalArgumentException ex) {
                        callback.accept(new ArrayList<>(), "The server returned an invalid song list.");
                        return;
                    }
                }
                if (!response.isNetworkError()) {
                    callback.accept(new ArrayList<>(), errorMessage(response, "Could not search songs on the server."));
                    return;
                }
            }
            SongsData helper = new SongsData();
            callback.accept(helper.performSearch(localSongs(), term, true), null);
        });
    }

    public static void filterByGenreAsync(String genre, BiConsumer<ArrayList<SongsData>, String> callback) {
        CompletableFuture.runAsync(() -> {
            if (ApiClient.isCloudAvailable()) {
                String path = "/api/songs/filter?genre=" + URLEncoder.encode(genre, StandardCharsets.UTF_8);
                ApiResponse response = ApiClient.get(path);
                if (response.isSuccess()) {
                    try {
                        callback.accept(parseJsonSongArray(response.getBody()), null);
                        return;
                    } catch (IllegalArgumentException ex) {
                        callback.accept(new ArrayList<>(), "The server returned an invalid song list.");
                        return;
                    }
                }
                if (!response.isNetworkError()) {
                    callback.accept(new ArrayList<>(), errorMessage(response, "Could not filter songs on the server."));
                    return;
                }
            }
            SongsData helper = new SongsData();
            helper.list = localSongs();
            callback.accept(helper.filterByGenre(genre), null);
        });
    }

    public static void addSongAsync(String title, String artist, String genre, int year,
                                    BiConsumer<Boolean, String> callback) {
        CompletableFuture.runAsync(() -> {
            if (ApiClient.isCloudAvailable()) {
                if (ApiClient.getAccessToken() == null) {
                    callback.accept(false, "Sign in with an administrator account to add songs.");
                    return;
                }
                ApiResponse response = ApiClient.postAuth("/api/songs", songJson(title, artist, genre, year));
                if (response.isSuccess()) {
                    callback.accept(true, null);
                } else {
                    callback.accept(false, errorMessage(response, "Could not add the song on the server."));
                }
                return;
            }
            SongsFileHandling local = new SongsFileHandling();
            local.addSong(new SongsData(0, title, artist, genre, year));
            callback.accept(true, "Saved locally. The backend is offline.");
        });
    }

    public static void editSongAsync(SongsData song, BiConsumer<Boolean, String> callback) {
        CompletableFuture.runAsync(() -> {
            if (ApiClient.isCloudAvailable()) {
                if (ApiClient.getAccessToken() == null || song.getId() <= 0) {
                    callback.accept(false, "A cloud song ID and administrator sign-in are required to edit this song.");
                    return;
                }
                ApiResponse response = ApiClient.put("/api/songs/" + song.getId(),
                        songJson(song.getSongName(), song.getArtistName(), song.getGenre(), song.getYear()));
                if (!response.isSuccess()) {
                    callback.accept(false, errorMessage(response, "Could not update the song on the server."));
                    return;
                }
            }
            new SongsFileHandling().saveEditedSong(song);
            callback.accept(true, ApiClient.isCloudAvailable() ? null : "Saved locally. The backend is offline.");
        });
    }

    public static void deleteSongAsync(int songId, BiConsumer<Boolean, String> callback) {
        CompletableFuture.runAsync(() -> {
            if (ApiClient.isCloudAvailable()) {
                if (ApiClient.getAccessToken() == null || songId <= 0) {
                    callback.accept(false, "A cloud song ID and administrator sign-in are required to delete this song.");
                    return;
                }
                ApiResponse response = ApiClient.delete("/api/songs/" + songId);
                if (!response.isSuccess()) {
                    callback.accept(false, errorMessage(response, "Could not delete the song on the server."));
                    return;
                }
            }
            new SongsFileHandling().deleteSong(songId);
            callback.accept(true, ApiClient.isCloudAvailable() ? null : "Deleted locally. The backend is offline.");
        });
    }

    static ArrayList<SongsData> parseJsonSongArray(String json) {
        List<Object> values = Json.array(Json.parse(json));
        if (values == null) throw new IllegalArgumentException("Expected a JSON array.");
        ArrayList<SongsData> result = new ArrayList<>();
        for (Object value : values) {
            Map<String, Object> fields = Json.object(value);
            if (fields == null) throw new IllegalArgumentException("Expected a song object.");
            SongsData song = new SongsData();
            song.setId((int) Json.number(fields, "id"));
            song.setSongName(Json.string(fields, "title"));
            song.setArtistName(Json.string(fields, "artist"));
            song.setGenre(Json.string(fields, "genre"));
            song.setYear((int) Json.number(fields, "releaseYear"));
            song.setAudioUrl(Json.string(fields, "audioUrl"));
            song.setCoverUrl(Json.string(fields, "coverUrl"));
            result.add(song);
        }
        return result;
    }

    private static ArrayList<SongsData> localSongs() {
        return new SongsFileHandling().getSongs();
    }

    private static String songJson(String title, String artist, String genre, int year) {
        return "{\"title\":" + ApiClient.jsonString(title) + ","
                + "\"artist\":" + ApiClient.jsonString(artist) + ","
                + "\"genre\":" + ApiClient.jsonString(genre) + ","
                + "\"releaseYear\":" + year + "}";
    }

    private static String errorMessage(ApiResponse response, String fallback) {
        String error = response.field("error");
        return error == null || error.isBlank() ? fallback : error;
    }

}
