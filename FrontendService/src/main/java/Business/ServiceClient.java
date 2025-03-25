package Business;

import java.io.StringReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * Client for communicating with other microservices
 */
public class ServiceClient {
    
    // Service URLs (implement later)
    private static final String AUTH_SERVICE_URL = "http://localhost:8080/AuthService";
    private static final String USER_SERVICE_URL = "http://localhost:8080/UserService";
    private static final String GOAL_SERVICE_URL = "http://localhost:8080/GoalService";
    
    /**
     * Authenticates a user with the Auth Service
     * 
     * @param email User's email
     * @param password User's password
     * @return JsonObject with authentication result or null if error
     */
    public static JsonObject authenticate(String email, String password) {
        try {
            System.out.println("Authenticating user: " + email);
            System.out.println("Using Auth Service URL: " + AUTH_SERVICE_URL);
            
            // Create client
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(AUTH_SERVICE_URL).path("/api/auth/login");
            
            // Create request body
            JsonObject requestBody = Json.createObjectBuilder()
                    .add("email", email)
                    .add("password", password)
                    .build();
            
            System.out.println("Sending authentication request...");
            
            // Send request
            Response response = target
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(requestBody.toString(), MediaType.APPLICATION_JSON));
            
            System.out.println("Auth response status: " + response.getStatus());
            
            if (response.getStatus() == 200) {
                // Parse response
                String jsonResponse = response.readEntity(String.class);
                System.out.println("Auth response: " + jsonResponse);
                
                try (JsonReader jsonReader = Json.createReader(new StringReader(jsonResponse))) {
                    return jsonReader.readObject();
                }
            } else {
                System.out.println("Authentication failed: " + response.getStatus());
                // Try to read error message if available
                try {
                    String errorResponse = response.readEntity(String.class);
                    System.out.println("Error details: " + errorResponse);
                } catch (Exception e) {
                    // Ignore if we can't read response
                }
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error calling Auth Service: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Gets user information from the User Service
     * 
     * @param email User's email
     * @param token Authentication token
     * @return JsonObject with user info or null if error
     */
    public static JsonObject getUserInfo(String email, String token) {
        try {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(USER_SERVICE_URL).path("/api/users").queryParam("email", email);
            
            // Send request
            Response response = target
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .get();
            
            if (response.getStatus() == 200) {
                // Parse response
                String jsonResponse = response.readEntity(String.class);
                try (JsonReader jsonReader = Json.createReader(new StringReader(jsonResponse))) {
                    return jsonReader.readObject();
                }
            } else {
                System.out.println("Get user info failed: " + response.getStatus());
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error calling User Service: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Creates a new user through the User Service
     * 
     * @param userInfo JSON with user information
     * @return true if user was created, false otherwise
     */
    public static boolean createUser(JsonObject userInfo) {
        try {
            System.out.println("Creating user: " + userInfo.getString("username"));
            System.out.println("Using User Service URL: " + USER_SERVICE_URL);
            
            // Create client
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(USER_SERVICE_URL).path("/api/users");
            
            // Send request
            Response response = target
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(userInfo.toString(), MediaType.APPLICATION_JSON));
            
            System.out.println("Create user response status: " + response.getStatus());
            
            if (response.getStatus() == 200 || response.getStatus() == 201) {
                // Parse response
                String jsonResponse = response.readEntity(String.class);
                System.out.println("Create user response: " + jsonResponse);
                
                try (JsonReader jsonReader = Json.createReader(new StringReader(jsonResponse))) {
                    JsonObject responseObj = jsonReader.readObject();
                    return responseObj.getBoolean("success", false);
                }
            } else {
                System.out.println("User creation failed: " + response.getStatus());
                try {
                    String errorResponse = response.readEntity(String.class);
                    System.out.println("Error details: " + errorResponse);
                } catch (Exception e) {
                    // Ignore if we can't read response
                }
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error calling User Service: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Creates a new goal through the Goal Service
     * 
     * @param goalInfo JSON with goal information
     * @param token Authentication token
     * @return true if goal was created, false otherwise
     */
    public static boolean createGoal(JsonObject goalInfo, String token) {
        try {
            System.out.println("Creating goal: " + goalInfo.getString("title"));
            System.out.println("Using Goal Service URL: " + GOAL_SERVICE_URL);
            
            // Create client
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(GOAL_SERVICE_URL).path("/api/goals");
            
            // Send request with Authorization header including the token
            Response response = target
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token) // Add JWT token to request
                    .post(Entity.entity(goalInfo.toString(), MediaType.APPLICATION_JSON));
            
            System.out.println("Create goal response status: " + response.getStatus());
            
            if (response.getStatus() == 200 || response.getStatus() == 201) {
                // Parse response
                String jsonResponse = response.readEntity(String.class);
                System.out.println("Create goal response: " + jsonResponse);
                
                try (JsonReader jsonReader = Json.createReader(new StringReader(jsonResponse))) {
                    JsonObject responseObj = jsonReader.readObject();
                    return responseObj.getBoolean("success", false);
                }
            } else {
                System.out.println("Goal creation failed: " + response.getStatus());
                try {
                    String errorResponse = response.readEntity(String.class);
                    System.out.println("Error details: " + errorResponse);
                } catch (Exception e) {
                    // Ignore if we can't read response
                }
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error calling Goal Service: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets all goals for the current user
     * 
     * @param token JWT token for authentication
     * @return JSON string with goals data
     */
    public static String getUserGoals(String token) {
        try {
            System.out.println("========== getUserGoals called ==========");
            System.out.println("Fetching goals with token: " + token.substring(0, Math.min(20, token.length())) + "...");
            System.out.println("Using Goal Service URL: " + GOAL_SERVICE_URL);
            
            // Create client
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(GOAL_SERVICE_URL).path("/api/goals");
            String fullUrl = target.getUri().toString();
            System.out.println("Full request URL: " + fullUrl);
            
            // Send request with Bearer token in Authorization header 
            Response response = target
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .get();
            
            System.out.println("Get goals response status: " + response.getStatus());
            
            if (response.getStatus() == 200) {
                String jsonResponse = response.readEntity(String.class);
                System.out.println("Goals response: " + jsonResponse);
                return jsonResponse;
            } else {
                System.out.println("Failed to get goals: " + response.getStatus());
                try {
                    String errorResponse = response.readEntity(String.class);
                    System.out.println("Error details: " + errorResponse);
                } catch (Exception e) {
                    // Ignore if we can't read response
                }
                return "[]";  // Return empty array on failure
            }
        } catch (Exception e) {
            System.out.println("Error calling Goal Service: " + e.getMessage());
            e.printStackTrace();
            return "[]";  // Return empty array on failure
        }
    }
    
    // More methods for communicating with other microservices
}
