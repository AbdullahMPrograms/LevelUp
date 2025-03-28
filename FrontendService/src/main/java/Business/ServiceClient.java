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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client for communicating with other microservices using environment variables.
 */
public class ServiceClient {

    private static final Logger LOGGER = Logger.getLogger(ServiceClient.class.getName());

    // Get service locations from environment variables or fallback to localhost:8080 (for testing locally for now)
    private static final String AUTH_SERVICE_HOSTPORT = System.getenv("AUTH_SERVICE_HOSTPORT") != null ? System.getenv("AUTH_SERVICE_HOSTPORT") : "localhost:8080";
    private static final String USER_SERVICE_HOSTPORT = System.getenv("USER_SERVICE_HOSTPORT") != null ? System.getenv("USER_SERVICE_HOSTPORT") : "localhost:8080";
    private static final String GOAL_SERVICE_HOSTPORT = System.getenv("GOAL_SERVICE_HOSTPORT") != null ? System.getenv("GOAL_SERVICE_HOSTPORT") : "localhost:8080";

    // Construct base URLs
    private static final String AUTH_SERVICE_URL = "http://" + AUTH_SERVICE_HOSTPORT + "/AuthService";
    private static final String USER_SERVICE_URL = "http://" + USER_SERVICE_HOSTPORT + "/UserService";
    private static final String GOAL_SERVICE_URL = "http://" + GOAL_SERVICE_HOSTPORT + "/GoalService";


    /**
     * Authenticates a user with the Auth Service
     *
     * @param email User's email
     * @param password User's password
     * @return JsonObject with authentication result or null if error
     */
    public static JsonObject authenticate(String email, String password) {
        String targetUrl = AUTH_SERVICE_URL + "/api/auth/login";
        LOGGER.log(Level.INFO, "Authenticating user {0} via URL: {1}", new Object[]{email, targetUrl});

        try {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(targetUrl); // Use constructed URL

            JsonObject requestBody = Json.createObjectBuilder()
                    .add("email", email)
                    .add("password", password)
                    .build();

            LOGGER.log(Level.INFO, "Sending authentication request to: {0}", targetUrl);
            Response response = target
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(requestBody.toString(), MediaType.APPLICATION_JSON));

            LOGGER.log(Level.INFO, "Auth response status from {0}: {1}", new Object[]{targetUrl, response.getStatus()});

             if (response.getStatus() == 200) {
                 String jsonResponse = response.readEntity(String.class);
                 LOGGER.log(Level.FINE, "Auth response body: {0}", jsonResponse);
                 try (JsonReader jsonReader = Json.createReader(new StringReader(jsonResponse))) {
                     return jsonReader.readObject();
                 }
             } else {
                 try {
                     String errorResponse = response.readEntity(String.class);
                     LOGGER.log(Level.WARNING, "Authentication failed ({0}): {1}", new Object[]{response.getStatus(), errorResponse});
                 } catch (Exception e) {
                     LOGGER.log(Level.WARNING, "Authentication failed ({0}), unable to read error body.", response.getStatus());
                 }
                 return null;
             }
         } catch (Exception e) {
              LOGGER.log(Level.SEVERE, "Error calling Auth Service at " + targetUrl, e);
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
        String targetUrl = USER_SERVICE_URL + "/api/users"; // Base URL
        LOGGER.log(Level.INFO, "Getting user info for {0} via URL: {1}", new Object[]{email, targetUrl});

        try {
            Client client = ClientBuilder.newClient();
            // Add query parameter correctly
            WebTarget target = client.target(targetUrl).queryParam("email", email);

            LOGGER.log(Level.INFO, "Sending get user info request to: {0}", target.getUri().toString()); // Log the full URI
            Response response = target
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .get();

            LOGGER.log(Level.INFO, "Get user info response status from {0}: {1}", new Object[]{target.getUri().toString(), response.getStatus()});

            if (response.getStatus() == 200) {
                String jsonResponse = response.readEntity(String.class);
                LOGGER.log(Level.FINE, "Get user info response body: {0}", jsonResponse);
                try (JsonReader jsonReader = Json.createReader(new StringReader(jsonResponse))) {
                    return jsonReader.readObject();
                }
            } else {
                try {
                     String errorResponse = response.readEntity(String.class);
                     LOGGER.log(Level.WARNING, "Get user info failed ({0}): {1}", new Object[]{response.getStatus(), errorResponse});
                 } catch (Exception e) {
                      LOGGER.log(Level.WARNING, "Get user info failed ({0}), unable to read error body.", response.getStatus());
                 }
                 return null;
             }
         } catch (Exception e) {
              LOGGER.log(Level.SEVERE, "Error calling User Service at " + targetUrl, e);
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
        String targetUrl = USER_SERVICE_URL + "/api/users";
        LOGGER.log(Level.INFO, "Creating user via URL: {0}", targetUrl);

        try {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(targetUrl);

            LOGGER.log(Level.INFO, "Sending create user request to: {0}", targetUrl);
            Response response = target
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(userInfo.toString(), MediaType.APPLICATION_JSON));

            LOGGER.log(Level.INFO, "Create user response status from {0}: {1}", new Object[]{targetUrl, response.getStatus()});

             if (response.getStatus() == 200 || response.getStatus() == 201) {
                 String jsonResponse = response.readEntity(String.class);
                 LOGGER.log(Level.FINE, "Create user response body: {0}", jsonResponse);
                 try (JsonReader jsonReader = Json.createReader(new StringReader(jsonResponse))) {
                     JsonObject responseObj = jsonReader.readObject();
                     return responseObj.getBoolean("success", false);
                 }
             } else {
                 try {
                     String errorResponse = response.readEntity(String.class);
                     LOGGER.log(Level.WARNING, "Create user failed ({0}): {1}", new Object[]{response.getStatus(), errorResponse});
                 } catch (Exception e) {
                      LOGGER.log(Level.WARNING, "Create user failed ({0}), unable to read error body.", response.getStatus());
                 }
                 return false;
             }
         } catch (Exception e) {
              LOGGER.log(Level.SEVERE, "Error calling User Service at " + targetUrl, e);
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
        String targetUrl = GOAL_SERVICE_URL + "/api/goals";
        LOGGER.log(Level.INFO, "Creating goal via URL: {0}", targetUrl);
         try {
             Client client = ClientBuilder.newClient();
             WebTarget target = client.target(targetUrl);

             LOGGER.log(Level.INFO, "Sending create goal request to: {0}", targetUrl);
             Response response = target
                     .request(MediaType.APPLICATION_JSON)
                     .header("Authorization", "Bearer " + token)
                     .post(Entity.entity(goalInfo.toString(), MediaType.APPLICATION_JSON));

             LOGGER.log(Level.INFO, "Create goal response status from {0}: {1}", new Object[]{targetUrl, response.getStatus()});

             if (response.getStatus() == 200 || response.getStatus() == 201) {
                 String jsonResponse = response.readEntity(String.class);
                  LOGGER.log(Level.FINE, "Create goal response body: {0}", jsonResponse);
                 try (JsonReader jsonReader = Json.createReader(new StringReader(jsonResponse))) {
                     JsonObject responseObj = jsonReader.readObject();
                     return responseObj.getBoolean("success", false);
                 }
             } else {
                  try {
                     String errorResponse = response.readEntity(String.class);
                      LOGGER.log(Level.WARNING, "Create goal failed ({0}): {1}", new Object[]{response.getStatus(), errorResponse});
                 } catch (Exception e) {
                      LOGGER.log(Level.WARNING, "Create goal failed ({0}), unable to read error body.", response.getStatus());
                 }
                 return false;
             }
         } catch (Exception e) {
              LOGGER.log(Level.SEVERE, "Error calling Goal Service at " + targetUrl, e);
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
        String targetUrl = GOAL_SERVICE_URL + "/api/goals";
        LOGGER.log(Level.INFO, "Getting goals via URL: {0}", targetUrl);

         try {
             Client client = ClientBuilder.newClient();
             WebTarget target = client.target(targetUrl);

             LOGGER.log(Level.INFO, "Sending get goals request to: {0}", targetUrl);
             Response response = target
                     .request(MediaType.APPLICATION_JSON)
                     .header("Authorization", "Bearer " + token)
                     .get();

             LOGGER.log(Level.INFO, "Get goals response status from {0}: {1}", new Object[]{targetUrl, response.getStatus()});

              if (response.getStatus() == 200) {
                 String jsonResponse = response.readEntity(String.class);
                  LOGGER.log(Level.FINE, "Get goals response body: {0}", jsonResponse);
                 return jsonResponse;
             } else {
                   try {
                     String errorResponse = response.readEntity(String.class);
                       LOGGER.log(Level.WARNING, "Get goals failed ({0}): {1}", new Object[]{response.getStatus(), errorResponse});
                 } catch (Exception e) {
                      LOGGER.log(Level.WARNING, "Get goals failed ({0}), unable to read error body.", response.getStatus());
                 }
                 return "[]"; // Return empty JSON array on failure
             }
         } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error calling Goal Service at " + targetUrl, e);
             return "[]"; // Return empty JSON array on failure
         }
     }
}
