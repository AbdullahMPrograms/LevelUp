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

/**
 * Data access model for authentication
 */
public class AuthPersistence {
    
    private static final String USER_SERVICE_URL = "http://localhost:8080/UserService/api/users";
    
    /**
     * Call the UserService API to get user information
     * 
     * @param email User email
     * @return JsonObject containing user data or null if not found/error
     */
    private static JsonObject getUserFromService(String email) {
        try {
            // URL encode the email parameter
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
            URL url = new URL(USER_SERVICE_URL + "?email=" + encodedEmail);
            
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            int responseCode = conn.getResponseCode();
            System.out.println("UserService API Response Code: " + responseCode);
            
            if (responseCode == 200) { // Success
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                // Parse the JSON response
                JsonReader jsonReader = Json.createReader(new StringReader(response.toString()));
                JsonObject jsonObject = jsonReader.readObject();
                jsonReader.close();
                
                return jsonObject;
            } else {
                System.out.println("Failed to get user data from UserService");
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error calling UserService API: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Verify user credentials by calling UserService API
     * 
     * @param email User email
     * @param password User password
     * @return Object array with [isAuthenticated, username] or null if error
     */
    public static Object[] authenticateUser(String email, String password) {
        try {
            // Get user data from UserService
            JsonObject userData = getUserFromService(email);
            
            if (userData == null || userData.containsKey("error")) {
                System.out.println("User not found or error from UserService");
                return new Object[] { false, null };
            }
            
            // Extract user information
            String username = userData.getString("username");
            
            // Create a second API call to verify credentials
            URL authUrl = new URL(USER_SERVICE_URL + "/authenticate");
            HttpURLConnection conn = (HttpURLConnection) authUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            
            // Create JSON payload with email and password
            String jsonPayload = Json.createObjectBuilder()
                .add("email", email)
                .add("password", password)
                .build().toString();
            
            conn.getOutputStream().write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            
            int responseCode = conn.getResponseCode();
            System.out.println("Authentication API Response Code: " + responseCode);
            
            boolean isAuthenticated = false;
            
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                // Parse the JSON response
                JsonReader jsonReader = Json.createReader(new StringReader(response.toString()));
                JsonObject authResponse = jsonReader.readObject();
                jsonReader.close();
                
                isAuthenticated = authResponse.getBoolean("authenticated", false);
            }
            
            System.out.println("Authentication result for " + email + ": " + isAuthenticated);
            return new Object[] { isAuthenticated, username };
            
        } catch (Exception e) {
            System.out.println("Error authenticating user: " + e.getMessage());
            e.printStackTrace();
            return new Object[] { false, null };
        }
    }
    
    /**
     * Get user information by email
     * 
     * @param email User email
     * @return Object array with [userId, username, email] or null if not found
     */
    public static Object[] getUserByEmail(String email) {
        try {
            // Get user data from UserService
            JsonObject userData = getUserFromService(email);
            
            if (userData == null || userData.containsKey("error")) {
                return null;
            }
            
            // Extract user information
            int userId = userData.getInt("id");
            String username = userData.getString("username");
            String userEmail = userData.getString("email");
            
            return new Object[] { userId, username, userEmail };
            
        } catch (Exception e) {
            System.out.println("Error getting user by email: " + e.getMessage());
            return null;
        }
    }
}
