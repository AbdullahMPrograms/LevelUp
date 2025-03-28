package Persistence;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access model for authentication (communicates with UserService).
 */
public class AuthPersistence {

    private static final Logger LOGGER = Logger.getLogger(AuthPersistence.class.getName());

    // Get UserService location from environment variable with local default
    private static final String USER_SERVICE_HOSTPORT = System.getenv("USER_SERVICE_HOSTPORT") != null ? System.getenv("USER_SERVICE_HOSTPORT") : "localhost:8080";
    private static final String USER_SERVICE_BASE_URL = "http://" + USER_SERVICE_HOSTPORT + "/UserService";


    /**
     * Call the UserService API to get user information by email.
     *
     * @param email User email
     * @return JsonObject containing user data or null if not found/error
     */
    private static JsonObject getUserFromService(String email) {
        HttpURLConnection conn = null;
        String encodedEmail = "";
        String targetUrlStr = "";

        try {
            // Construct the target URL
            encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
            targetUrlStr = USER_SERVICE_BASE_URL + "/api/users?email=" + encodedEmail;
            URL url = new URL(targetUrlStr);
            LOGGER.log(Level.INFO, "Calling UserService at: {0}", targetUrlStr);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            LOGGER.log(Level.INFO, "UserService GET response code: {0}", responseCode);

            if (responseCode == 200) { // Success
                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }
                 LOGGER.log(Level.FINE, "UserService GET response body: {0}", response.toString());
                // Parse the JSON response
                try (JsonReader jsonReader = Json.createReader(new StringReader(response.toString()))) {
                    return jsonReader.readObject();
                }
            } else {
                 LOGGER.log(Level.WARNING, "Failed to get user data from UserService ({0}) for email: {1}", new Object[]{responseCode, email});
                 // Optionally read error stream
                return null;
            }
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error calling UserService API at " + targetUrlStr, e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Verify user credentials by calling UserService's /authenticate endpoint.
     *
     * @param email User email
     * @param password User password
     * @return Object array with [isAuthenticated, username] or [false, null] if error/not authenticated.
     */
    public static Object[] authenticateUser(String email, String password) {
        HttpURLConnection conn = null;
        String targetUrlStr = "";
         LOGGER.log(Level.INFO, "Authenticating user via UserService: {0}", email);

        try {
            // First, ensure the user *exists* by fetching their data (includes username)
            JsonObject userData = getUserFromService(email);
            if (userData == null || userData.containsKey("error")) {
                 LOGGER.log(Level.WARNING, "User not found via GET or error from UserService for email: {0}", email);
                return new Object[] { false, null };
            }
            // Extract username now, before the authentication call
            String username = userData.getString("username", null); // Get username

            // Now, call the /authenticate endpoint to verify the password
            targetUrlStr = USER_SERVICE_BASE_URL + "/api/users/authenticate";
            URL authUrl = new URL(targetUrlStr);
             LOGGER.log(Level.INFO, "Calling UserService authentication endpoint: {0}", targetUrlStr);

            conn = (HttpURLConnection) authUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Create JSON payload
            String jsonPayload = Json.createObjectBuilder()
                .add("email", email)
                .add("password", password)
                .build().toString();

            // Send payload
            conn.getOutputStream().write(jsonPayload.getBytes(StandardCharsets.UTF_8));

            int responseCode = conn.getResponseCode();
             LOGGER.log(Level.INFO, "UserService POST /authenticate response code: {0}", responseCode);

            boolean isAuthenticated = false;
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }
                 LOGGER.log(Level.FINE, "UserService POST /authenticate response body: {0}", response.toString());
                // Parse the JSON response
                try (JsonReader jsonReader = Json.createReader(new StringReader(response.toString()))) {
                    JsonObject authResponse = jsonReader.readObject();
                    isAuthenticated = authResponse.getBoolean("authenticated", false);
                }
            } else {
                 LOGGER.log(Level.WARNING, "Authentication check via UserService failed ({0}) for email: {1}", new Object[]{responseCode, email});
            }

             LOGGER.log(Level.INFO, "Authentication result for {0}: {1}", new Object[]{email, isAuthenticated});
            // Return username only if authenticated
            return new Object[] { isAuthenticated, (isAuthenticated ? username : null) };

        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error during authenticateUser call to " + targetUrlStr, e);
            return new Object[] { false, null }; // Indicate failure
        } finally {
             if (conn != null) {
                 conn.disconnect();
             }
        }
    }

     // getUserByEmail method remains primarily for internal use within authenticateUser now
     public static Object[] getUserByEmail(String email) {
         JsonObject userData = getUserFromService(email);
         if (userData == null || userData.containsKey("error")) {
             return null;
         }
         try {
             int userId = userData.getInt("id");
             String username = userData.getString("username");
             String userEmail = userData.getString("email");
             return new Object[] { userId, username, userEmail };
         } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error parsing user data from UserService response", e);
             return null;
         }
     }
}
