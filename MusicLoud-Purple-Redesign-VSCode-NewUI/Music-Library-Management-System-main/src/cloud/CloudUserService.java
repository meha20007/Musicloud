package cloud;

import cloud.ApiClient.ApiResponse;
import code.User;
import code.UserFileHandling;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Admin user list for the Swing frontend.
 * Online: GET/DELETE /api/users. Offline: the existing local user file.
 */
public class CloudUserService {

    public static void loadAllAsync(BiConsumer<ArrayList<User>, String> callback) {
        CompletableFuture.runAsync(() -> {
            if (ApiClient.isCloudAvailable() && ApiClient.getAccessToken() != null) {
                try {
                    ApiResponse res = ApiClient.get("/api/users");
                    if (res.isSuccess()) {
                        callback.accept(parseUsers(res.getBody()), null);
                        return;
                    }
                    String error = res.field("error");
                    callback.accept(localUsers(), error != null ? error : "Could not load users from the server.");
                    return;
                } catch (Exception e) {
                    // fall through to local
                }
            }
            callback.accept(localUsers(), null);
        });
    }

    public static void deleteAsync(String username, BiConsumer<Boolean, String> callback) {
        CompletableFuture.runAsync(() -> {
            if (ApiClient.isCloudAvailable() && ApiClient.getAccessToken() != null) {
                try {
                    String path = "/api/users/" + URLEncoder.encode(username, StandardCharsets.UTF_8);
                    ApiResponse res = ApiClient.delete(path);
                    if (!res.isSuccess()) {
                        String error = res.field("error");
                        callback.accept(false, error != null ? error : "Could not delete that account on the server.");
                        return;
                    }
                } catch (Exception e) {
                    callback.accept(false, "Network error: " + e.getMessage());
                    return;
                }
            }
            new UserFileHandling().deleteUser(username);
            callback.accept(true, null);
        });
    }

    private static ArrayList<User> localUsers() {
        return new UserFileHandling().getUsers();
    }

    static ArrayList<User> parseUsers(String json) {
        List<Object> values = Json.array(Json.parse(json));
        if (values == null) throw new IllegalArgumentException("Expected a JSON array.");
        ArrayList<User> result = new ArrayList<>();
        for (Object value : values) {
            Map<String, Object> userData = Json.object(value);
            if (userData == null) throw new IllegalArgumentException("Expected a user object.");
            String username = Json.string(userData, "username");
            if (username == null || username.isBlank()) continue;
            String fullName = Json.string(userData, "fullName");
            String email = Json.string(userData, "email");
            String first = fullName == null ? username : fullName;
            String last = "";
            if (fullName != null) {
                int space = fullName.indexOf(' ');
                if (space > 0) {
                    first = fullName.substring(0, space);
                    last = fullName.substring(space + 1);
                }
            }
            result.add(new User(username, "", first, last, email == null ? "" : email));
        }
        return result;
    }
}
