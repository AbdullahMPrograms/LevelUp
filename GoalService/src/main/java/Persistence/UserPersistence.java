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
 * Model for user-related operations using UserService API
 */
public class UserPersistence {
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
     * Gets user ID from email address by calling UserService API
     * 
     * @param email The user's email
     * @return User ID if found, -1 otherwise
     */
    public static int getUserIdFromEmail(String email) {
        if (email == null || email.isEmpty()) {
            System.out.println("Empty email provided to getUserIdFromEmail");
            return -1;
        }
        
        System.out.println("Looking up user ID for email: " + email);
        
        try {
            // Call UserService to get user data
            JsonObject userData = getUserFromService(email);
            
            if (userData != null && !userData.containsKey("error")) {
                int userId = userData.getInt("id");
                System.out.println("Found user ID: " + userId + " for email: " + email);
                return userId;
            } else {
                System.out.println("No user found with email: " + email + " or error in response");
                return -1;
            }
        } catch (Exception e) {
            System.out.println("Error getting user by email: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
}
