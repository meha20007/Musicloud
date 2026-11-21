package cloud;

import cloud.ApiClient.ApiResponse;
import code.User;
import code.UserFileHandling;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Cloud Authentication Service for the Swing frontend.
 *
 * SIMPLE EXPLANATION:
 *   - Wraps the cloud API calls for login and registration.
 *   - If the cloud is not available, automatically falls back to
 *     your existing local file-based login (so the app always works!).
 *   - All network calls run in a background thread so the UI never freezes.
 *
 * HOW TO USE in your Swing code:
 *   CloudAuthService.loginAsync(username, password, (success, errorMsg) -> {
 *       SwingUtilities.invokeLater(() -> {
 *           if (success) showDashboard();
 *           else JOptionPane.showMessageDialog(null, errorMsg);
 *       });
 *   });
 */
public class CloudAuthService {

    /**
     * Log in asynchronously.
     * callback(true, null) on success.
     * callback(false, "error message") on failure.
     */
    public static void loginAsync(String usernameOrEmail, String password,
                                   BiConsumer<Boolean, String> callback) {
        CompletableFuture.runAsync(() -> {
            ApiClient.checkHealth();
            // ---- Try cloud first ----
            if (ApiClient.isCloudAvailable()) {
                try {
                    String body = ApiClient.json(
                            "usernameOrEmail", usernameOrEmail,
                            "password", password
                    );
                    ApiResponse res = ApiClient.post("/api/auth/login", body);

                    if (res.isSuccess()) {
                        // Store session in ApiClient
                        ApiClient.setSession(
                                res.field("accessToken"),
                                res.field("userId"),
                                res.field("username"),
                                res.field("fullName"),
                                res.field("role")
                        );
                        gui.PanelLoginAndRegister.loggedInUserFullName = ApiClient.getCurrentFullName();
                        gui.PanelLoginAndRegister.userN = ApiClient.getCurrentUsername();
                        callback.accept(true, null);
                        return;
                    } else if (res.isNetworkError()) {
                        // Fall through to local login
                    } else {
                        // Real auth error (wrong password, not found)
                        String error = res.field("error");
                        callback.accept(false, error != null ? error : "Login failed. Please check your credentials.");
                        return;
                    }
                } catch (Exception e) {
                    // Network error — fall through to local
                }
            }

            // ---- Fallback to local file-based login ----
            boolean localSuccess = localLogin(usernameOrEmail, password);
            if (localSuccess) {
                callback.accept(true, null);
            } else {
                callback.accept(false, "Login unsuccessful. Check your username/email and password.");
            }
        });
    }

    /**
     * Register a new account asynchronously.
     * callback(true, null) on success.
     * callback(false, "error message") on failure.
     */
    public static void registerAsync(String firstName, String lastName,
                                      String email, String username, String password,
                                      BiConsumer<Boolean, String> callback) {
        CompletableFuture.runAsync(() -> {
            ApiClient.checkHealth();
            // ---- Try cloud first ----
            if (ApiClient.isCloudAvailable()) {
                try {
                    String body = ApiClient.json(
                            "firstName", firstName,
                            "lastName", lastName,
                            "email", email,
                            "username", username,
                            "password", password
                    );
                    ApiResponse res = ApiClient.post("/api/auth/register", body);

                    if (res.isSuccess()) {
                        // Also save to local file for offline fallback
                        saveToLocalFile(firstName, lastName, email, username, password);

                        ApiClient.setSession(
                                res.field("accessToken"),
                                res.field("userId"),
                                res.field("username"),
                                res.field("fullName"),
                                res.field("role")
                        );
                        callback.accept(true, null);
                        return;
                    } else if (res.isConflict()) {
                        String error = res.field("error");
                        callback.accept(false, error != null ? error : "That email or username is already registered.");
                        return;
                    } else if (!res.isNetworkError()) {
                        String error = res.field("error");
                        callback.accept(false, error != null ? error : "Registration failed. Please try again.");
                        return;
                    }
                    // Network error — fall through to local only
                } catch (Exception e) {
                    // Network error — fall through
                }
            }

            // ---- Fallback: save to local file only ----
            try {
                saveToLocalFile(firstName, lastName, email, username, password);
                // Set minimal session for offline usage
                ApiClient.setSession(
                        null, // no JWT in offline mode
                        username,
                        username,
                        firstName + " " + lastName,
                        "USER"
                );
                callback.accept(true, null);
            } catch (Exception ex) {
                callback.accept(false, "Could not create account: " + ex.getMessage());
            }
        });
    }

    // ---- Local helpers ----

    private static boolean localLogin(String input, String password) {
        // Handle admin
        if ("admin".equalsIgnoreCase(input) && "admin".equals(password)) {
            ApiClient.setSession(null, "admin", "admin", "Administrator", "ADMIN");
            return true;
        }
        // Check local user file
        UserFileHandling h = new UserFileHandling();
        for (User user : h.getUsers()) {
            if ((user.getUserName().equals(input) || user.getEmail().equals(input))
                    && user.getPassword().equals(password)) {
                ApiClient.setSession(null, user.getUserName(),
                        user.getUserName(), user.getFullName(), "USER");
                // Also mirror into old static fields for backwards compatibility
                gui.PanelLoginAndRegister.loggedInUserFullName = user.getFullName();
                gui.PanelLoginAndRegister.userN = user.getUserName();
                return true;
            }
        }
        return false;
    }

    private static void saveToLocalFile(String firstName, String lastName,
                                         String email, String username, String password) {
        UserFileHandling h = new UserFileHandling();
        ArrayList<User> users = h.getUsers();
        users.add(new User(username, password, firstName, lastName, email));
        h.saveUserData(users);
    }

}
