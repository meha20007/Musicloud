package cloud;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.prefs.Preferences;

/**
 * ApiClient — the bridge between your Java Swing desktop app and the
 * Spring Boot cloud backend.
 *
 * SIMPLE EXPLANATION:
 *   Think of this like a phone. The Swing UI "dials" this class whenever
 *   it needs to talk to the cloud. It handles:
 *     - Sending the JWT token automatically with every request
 *     - Reading the JSON response back
 *     - Falling back gracefully if the backend is not reachable
 *
 * NO EXTRA LIBRARIES NEEDED — uses Java's built-in HttpClient (Java 11+).
 */
public class ApiClient {

    // ------------------------------------------------------------------ config
    /**
     * Base URL of your running Spring Boot backend.
     * Change this to your Render.com URL when deployed:
     *   e.g., "https://musicloud-backend.onrender.com"
     * Leave as localhost for local development.
     */
    public static String BASE_URL = readBaseUrl();

    private static final int TIMEOUT_SECONDS = 10;

    // ------------------------------------------------------------------ state
    private static String accessToken = null;   // JWT stored in memory only
    private static String currentUserId = null;
    private static String currentUsername = null;
    private static String currentFullName = null;
    private static String currentRole = null;
    private static boolean cloudAvailable = false;

    // Key for saving BASE_URL preference across runs
    private static final String PREF_BASE_URL = "musicloud_base_url";

    // HttpClient (stateless — reuse for all requests)
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    // ------------------------------------------------------------------ URL config
    private static String readBaseUrl() {
        String configuredUrl = System.getProperty("musicloud.api.url");
        if (configuredUrl == null || configuredUrl.isBlank()) {
            configuredUrl = System.getenv("MUSICLOUD_API_URL");
        }
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return normalizeBaseUrl(configuredUrl);
        }
        Preferences prefs = Preferences.userRoot().node("musicloud");
        return prefs.get(PREF_BASE_URL, "http://localhost:8080");
    }

    public static void setBaseUrl(String url) {
        BASE_URL = normalizeBaseUrl(url);
        Preferences prefs = Preferences.userRoot().node("musicloud");
        prefs.put(PREF_BASE_URL, BASE_URL);
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Backend URL is required.");
        String normalized = url.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        URI.create(normalized);
        return normalized;
    }

    // ------------------------------------------------------------------ auth helpers
    public static boolean isCloudAvailable()     { return cloudAvailable; }
    public static String  getAccessToken()        { return accessToken; }
    public static String  getCurrentUserId()      { return currentUserId; }
    public static String  getCurrentUsername()    { return currentUsername; }
    public static String  getCurrentFullName()    { return currentFullName; }
    public static String  getCurrentRole()        { return currentRole; }
    public static boolean isAdmin()               { return "ADMIN".equalsIgnoreCase(currentRole); }

    public static void clearSession() {
        accessToken = null; currentUserId = null;
        currentUsername = null; currentFullName = null; currentRole = null;
    }

    /** Store auth session after successful login */
    public static void setSession(String token, String userId, String username,
                                   String fullName, String role) {
        accessToken    = token;
        currentUserId  = userId;
        currentUsername = username;
        currentFullName = fullName;
        currentRole    = role;
    }

    // ------------------------------------------------------------------ health check
    /**
     * Ping the backend to see if it is online.
     * Returns true if reachable, false otherwise.
     * Call this at startup in a background thread.
     */
    public static boolean checkHealth() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            cloudAvailable = (res.statusCode() == 200);
        } catch (Exception e) {
            cloudAvailable = false;
        }
        return cloudAvailable;
    }

    // ------------------------------------------------------------------ POST (no auth)
    /**
     * Send a POST request with JSON body — no JWT needed (for login/register).
     * Returns the raw JSON response body, or null if the request failed.
     */
    public static ApiResponse post(String path, String jsonBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(res.statusCode(), res.body());
        } catch (Exception e) {
            return ApiResponse.networkError(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ GET (auth required)
    public static ApiResponse get(String path) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .GET();
            if (accessToken != null) builder.header("Authorization", "Bearer " + accessToken);
            HttpResponse<String> res = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(res.statusCode(), res.body());
        } catch (Exception e) {
            return ApiResponse.networkError(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ POST (auth required)
    public static ApiResponse postAuth(String path, String jsonBody) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
            if (accessToken != null) builder.header("Authorization", "Bearer " + accessToken);
            HttpResponse<String> res = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(res.statusCode(), res.body());
        } catch (Exception e) {
            return ApiResponse.networkError(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ PUT (auth required)
    public static ApiResponse put(String path, String jsonBody) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
            if (accessToken != null) builder.header("Authorization", "Bearer " + accessToken);
            HttpResponse<String> res = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(res.statusCode(), res.body());
        } catch (Exception e) {
            return ApiResponse.networkError(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ DELETE (auth required)
    public static ApiResponse delete(String path) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .DELETE();
            if (accessToken != null) builder.header("Authorization", "Bearer " + accessToken);
            HttpResponse<String> res = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(res.statusCode(), res.body());
        } catch (Exception e) {
            return ApiResponse.networkError(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ helper: simple JSON builder
    /**
     * Very lightweight JSON builder — avoids needing a Jackson/Gson dependency in Swing.
     * Usage: ApiClient.json("key1", "val1", "key2", "val2")
     * Returns: {"key1":"val1","key2":"val2"}
     */
    public static String json(String... keyValues) {
        if (keyValues.length % 2 != 0) throw new IllegalArgumentException("Must pass key-value pairs.");
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append(jsonString(keyValues[i])).append(":").append(jsonString(keyValues[i + 1]));
        }
        sb.append("}");
        return sb.toString();
    }

    public static String jsonString(String value) {
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char current = s.charAt(i);
            switch (current) {
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\b': escaped.append("\\b"); break;
                case '\f': escaped.append("\\f"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (current < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) current));
                    } else {
                        escaped.append(current);
                    }
            }
        }
        return escaped.toString();
    }

    // ------------------------------------------------------------------ inner response class
    public static class ApiResponse {
        private final int statusCode;
        private final String body;
        private final boolean networkError;

        public ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
            this.networkError = false;
        }

        private ApiResponse(int statusCode, String body, boolean networkError) {
            this.statusCode = statusCode;
            this.body = body;
            this.networkError = networkError;
        }

        public static ApiResponse networkError(String message) {
            return new ApiResponse(0, "{\"error\":\"" + escape(message) + "\"}", true);
        }

        public int getStatusCode()      { return statusCode; }
        public String getBody()         { return body; }
        public boolean isNetworkError() { return networkError; }
        public boolean isSuccess()      { return statusCode >= 200 && statusCode < 300; }
        public boolean isUnauthorized() { return statusCode == 401; }
        public boolean isConflict()     { return statusCode == 409; }

        /** Extract value of a JSON string field from the raw body — no Gson/Jackson needed */
        public String field(String key) {
            if (body == null) return null;
            try {
                return Json.string(Json.object(Json.parse(body)), key);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        /** Extract a numeric JSON field */
        public String numericField(String key) {
            if (body == null) return null;
            try {
                Object value = Json.object(Json.parse(body)).get(key);
                return value == null ? null : String.valueOf(value);
            } catch (IllegalArgumentException | NullPointerException ex) {
                return null;
            }
        }

        @Override
        public String toString() {
            return "ApiResponse[" + statusCode + "]: " + body;
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
