package Business;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.logging.Level; 
import java.util.logging.Logger;

import Helper.GoalInfo;
import Persistence.GoalPersistence;
//import Persistence.UserPersistence;

/**
 * Servlet for goal endpoints
 */
@WebServlet(name = "GoalServlet", urlPatterns = {"/api/goals/*", "/api/goals"})
public class GoalServlet extends HttpServlet {
    
    private static final String SECRET_KEY = "abcdefghijklmnopqrstuvwxyz1234567890";
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    private static final Logger LOGGER = Logger.getLogger(GoalServlet.class.getName());


    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("GoalServlet initializing...");
        System.out.println("Secret key first 5 chars: " + SECRET_KEY.substring(0, 5) + "...");
    }

    /**
     * Extracts user ID from request.
     */
    private int extractUserID(HttpServletRequest request) {
        // Check Authorization Header via JWT and local cache lookup
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                LOGGER.log(Level.INFO, "Found JWT token in Authorization header. Verifying...");

                Jws<Claims> jws = Jwts.parserBuilder()
                        .setSigningKey(SIGNING_KEY)
                        .build()
                        .parseClaimsJws(token);

                String username = jws.getBody().getSubject(); // Username from token subject
                String email = jws.getBody().get("email", String.class); // Email from token claim

                if (email == null || email.isEmpty()) {
                    LOGGER.log(Level.WARNING, "JWT is valid but missing 'email' claim.");
                    return -1;
                }

                LOGGER.log(Level.INFO, "JWT verified for user: {0}, email: {1}. Looking up UserID in local cache.", new Object[]{username, email});

                int userId = GoalPersistence.getUserIdFromCacheByEmail(email);

                if (userId > 0) {
                    LOGGER.log(Level.INFO, "Successfully found UserID {0} in local cache for email {1}", new Object[]{userId, email});
                    return userId;
                } else {
                    LOGGER.log(Level.WARNING, "UserID for email {0} not found in local cache. Authentication denied.", email);
                    return -1;
                }

            } catch (JwtException e) {
                LOGGER.log(Level.WARNING, "Invalid or expired JWT token: {0}", e.getMessage());
                return -1;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing JWT or querying cache: {0}", e.getMessage());
                return -1;
            }
        }

        // No valid authentication found
         LOGGER.log(Level.WARNING, "No valid authentication (JWT or userID param) found in request.");
        return -1;
    }

    /**
     * Handle GET requests - retrieve goals
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
         LOGGER.info("GoalServlet GET request received");
        try {
           // Extract user ID from request
           int userID = extractUserID(request);
           if (userID <= 0) {
                LOGGER.log(Level.WARNING, "GET request failed: User not authenticated or found in cache.");
                sendErrorResponse(response, "User not authenticated or user data not available.", HttpServletResponse.SC_UNAUTHORIZED);
               return;
           }

            LOGGER.log(Level.INFO, "GET request for goals - User ID: {0} (from cache/JWT)", userID);

           // Get goals from business layer
           List<GoalInfo> goals = GoalBusiness.getUserGoals(userID);

           // Send response
           response.setContentType("application/json");
           response.setCharacterEncoding("UTF-8");

           try (PrintWriter out = response.getWriter()) {
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < goals.size(); i++) {
                    if (i > 0) jsonBuilder.append(",");
                    GoalInfo goal = goals.get(i);
                    // Escape strings properly
                    jsonBuilder.append("{")
                        .append("\"id\":\"").append(goal.getUserID()).append("\",")
                        .append("\"title\":\"").append(escapeJson(goal.getTitle())).append("\",")
                        .append("\"targetDate\":\"").append(escapeJson(goal.getDate())).append("\",")
                        .append("\"metricType\":\"").append(escapeJson(goal.getMetricType())).append("\",")
                        .append("\"targetValue\":\"").append(escapeJson(goal.getTargetValue())).append("\",")
                        .append("\"targetUnit\":\"").append(escapeJson(goal.getTargetUnit())).append("\",")
                        .append("\"frequency\":\"").append(escapeJson(goal.getFrequency())).append("\",")
                        .append("\"description\":\"").append(escapeJson(goal.getDescription())).append("\"")
                        .append("}");
                }
                jsonBuilder.append("]");
                LOGGER.log(Level.INFO, "Sending JSON response with {0} goals for UserID {1}", new Object[]{goals.size(), userID});
                out.print(jsonBuilder.toString());
           }
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error in GoalServlet doGet: {0}", e.getMessage());
             sendErrorResponse(response, "Internal server error: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Handle POST requests - create goals
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
         LOGGER.info("GoalServlet POST request received");
         try {
            int userID = extractUserID(request);
            if (userID <= 0) {
                LOGGER.log(Level.WARNING, "POST request failed: User not authenticated or found in cache.");
                sendErrorResponse(response, "User not authenticated or user data not available.", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

             LOGGER.log(Level.INFO, "POST request to create goal - User ID: {0} (from cache/JWT)", userID);

            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            // Parse JSON
            JsonObject jsonRequest;
            try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
                jsonRequest = jsonReader.readObject();
            } catch (Exception e) {
                 LOGGER.log(Level.WARNING, "Invalid JSON format received: {0}", e.getMessage());
                 sendErrorResponse(response, "Invalid JSON format", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Extract goal info
            try {
                String title = jsonRequest.getString("title", "");
                String date = jsonRequest.getString("date", "");
                String metricType = jsonRequest.getString("metricType", "");
                String targetValue = jsonRequest.getString("targetValue", "");
                String targetUnit = jsonRequest.getString("targetUnit", "");
                String frequency = jsonRequest.getString("frequency", "");
                String description = jsonRequest.getString("description", "");

                // Validate required fields
                if (title.isEmpty() || date.isEmpty() || metricType.isEmpty() ||
                        targetValue.isEmpty() || targetUnit.isEmpty() || frequency.isEmpty()) {
                     LOGGER.log(Level.WARNING, "Goal creation failed: Missing required fields.");
                     sendErrorResponse(response, "Missing required fields", HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                // Create goal object, ASSOCIATING THE CORRECT USER ID
                GoalInfo newGoal = new GoalInfo(userID, title, date, metricType, targetValue,
                        targetUnit, frequency, description);

                // Create the goal
                String result = GoalBusiness.createGoal(newGoal);

                // Send response
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                if (result.contains("success")) { // Check based on the message from GoalBusiness
                     LOGGER.log(Level.INFO, "Goal created successfully for UserID: {0}", userID);
                    JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                            .add("success", true)
                            .add("message", "Goal created successfully");
                    try (PrintWriter out = response.getWriter()) { out.print(responseBuilder.build().toString()); }
                } else {
                     LOGGER.log(Level.WARNING, "Goal creation failed for UserID {0}: {1}", new Object[]{userID, result});
                    JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                            .add("success", false)
                            .add("message", result); // Pass the error message from business layer
                    try (PrintWriter out = response.getWriter()) { out.print(responseBuilder.build().toString()); }
                }

            } catch (Exception e) {
                 LOGGER.log(Level.SEVERE, "Error processing goal data: {0}", e.getMessage());
                 sendErrorResponse(response, "Error processing goal data", HttpServletResponse.SC_BAD_REQUEST);
            }

        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error in GoalServlet doPost: {0}", e.getMessage());
             sendErrorResponse(response, "Internal server error: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send error response in JSON format
     */
    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                .add("success", false)
                .add("message", message);
        try (PrintWriter out = response.getWriter()) { 
            out.print(responseBuilder.build().toString()); 
        }
    }
    
    /**
     * Escape JSON strings
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                    .replace("\r", "\\r").replace("\t", "\\t");
   }

    @Override
    public String getServletInfo() {
        return "GoalService API";
    }
}