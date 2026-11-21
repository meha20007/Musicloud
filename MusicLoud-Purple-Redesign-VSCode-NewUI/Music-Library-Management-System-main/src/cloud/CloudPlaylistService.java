package cloud;

import cloud.ApiClient.ApiResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Authenticated playlist API used by the Swing playlist screen. */
public class CloudPlaylistService {

    public static final class Playlist {
        private final Long id;
        private final String name;
        private final int songCount;

        private Playlist(Long id, String name, int songCount) {
            this.id = id;
            this.name = name;
            this.songCount = songCount;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public int getSongCount() { return songCount; }

        @Override
        public String toString() { return name; }
    }

    public static void loadPlaylistsAsync(BiConsumer<ArrayList<Playlist>, String> callback) {
        CompletableFuture.runAsync(() -> {
            if (!ApiClient.isCloudAvailable()) {
                callback.accept(new ArrayList<>(), "Connect to the backend and sign in to load playlists.");
                return;
            }
            if (ApiClient.getAccessToken() == null) {
                callback.accept(new ArrayList<>(), "Sign in to your MusicLoud account to load playlists.");
                return;
            }
            ApiResponse response = ApiClient.get("/api/playlists");
            if (!response.isSuccess()) {
                callback.accept(new ArrayList<>(), errorMessage(response, "Could not load playlists."));
                return;
            }
            try {
                callback.accept(parsePlaylistArray(response.getBody()), null);
            } catch (IllegalArgumentException ex) {
                callback.accept(new ArrayList<>(), "The server returned an invalid playlist list.");
            }
        });
    }

    public static void createPlaylistAsync(String name, BiConsumer<Playlist, String> callback) {
        CompletableFuture.runAsync(() -> {
            String signInError = signInError();
            if (signInError != null) {
                callback.accept(null, signInError);
                return;
            }
            ApiResponse response = ApiClient.postAuth("/api/playlists", ApiClient.json("name", name));
            if (!response.isSuccess()) {
                callback.accept(null, errorMessage(response, "Could not create the playlist."));
                return;
            }
            try {
                Map<String, Object> object = Json.object(Json.parse(response.getBody()));
                if (object == null) throw new IllegalArgumentException("Expected a playlist object.");
                callback.accept(parsePlaylist(object), null);
            } catch (IllegalArgumentException ex) {
                callback.accept(null, "The server returned an invalid playlist.");
            }
        });
    }

    public static void deletePlaylistAsync(Long playlistId, BiConsumer<Boolean, String> callback) {
        CompletableFuture.runAsync(() -> {
            String signInError = signInError();
            if (signInError != null) {
                callback.accept(false, signInError);
                return;
            }
            if (playlistId == null) {
                callback.accept(false, "This playlist has no server ID. Refresh the list and try again.");
                return;
            }
            ApiResponse response = ApiClient.delete("/api/playlists/" + playlistId);
            callback.accept(response.isSuccess(),
                    response.isSuccess() ? null : errorMessage(response, "Could not delete the playlist."));
        });
    }

    public static void addSongToPlaylistAsync(Long playlistId, Long songId,
                                               BiConsumer<Boolean, String> callback) {
        CompletableFuture.runAsync(() -> {
            String signInError = signInError();
            if (signInError != null) {
                callback.accept(false, signInError);
                return;
            }
            if (playlistId == null || songId == null || songId <= 0) {
                callback.accept(false, "Select a cloud playlist and a song from the server library.");
                return;
            }
            ApiResponse response = ApiClient.postAuth(
                    "/api/playlists/" + playlistId + "/songs/" + songId, "{}");
            callback.accept(response.isSuccess(),
                    response.isSuccess() ? null : errorMessage(response, "Could not add the song to the playlist."));
        });
    }

    private static ArrayList<Playlist> parsePlaylistArray(String json) {
        List<Object> values = Json.array(Json.parse(json));
        if (values == null) throw new IllegalArgumentException("Expected a JSON array.");
        ArrayList<Playlist> playlists = new ArrayList<>();
        for (Object value : values) {
            Map<String, Object> object = Json.object(value);
            if (object == null) throw new IllegalArgumentException("Expected a playlist object.");
            playlists.add(parsePlaylist(object));
        }
        return playlists;
    }

    private static Playlist parsePlaylist(Map<String, Object> object) {
        long id = Json.number(object, "id");
        String name = Json.string(object, "name");
        List<Object> songs = Json.array(object.get("songs"));
        return new Playlist(id > 0 ? id : null, name == null ? "" : name, songs == null ? 0 : songs.size());
    }

    private static String signInError() {
        if (!ApiClient.isCloudAvailable()) return "Connect to the backend to manage playlists.";
        if (ApiClient.getAccessToken() == null) return "Sign in to your MusicLoud account to manage playlists.";
        return null;
    }

    private static String errorMessage(ApiResponse response, String fallback) {
        String error = response.field("error");
        return error == null || error.isBlank() ? fallback : error;
    }
}
