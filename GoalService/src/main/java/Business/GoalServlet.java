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

import Helper.GoalInfo;
import Persistence.UserPersistence;

/**
 * Servlet for goal endpoints
 */
@WebServlet(name = "GoalServlet", urlPatterns = {"/api/goals/*", "/api/goals"})
public class GoalServlet extends HttpServlet {
    
    // MUST match the key in FrontendService's Authentication class
    private static final String SECRET_KEY = "abcdefghijklmnopqrstuvwxyz1234567890";
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("GoalServlet initializing...");
        System.out.println("Secret key first 5 chars: " + SECRET_KEY.substring(0, 5) + "...");
    }

    /**
     * Extracts user ID from request - either from query param or JWT token
     */
    private int extractUserID(HttpServletRequest request) {
        // First check for userID parameter (direct access for testing)
        String userIDStr = request.getParameter("userID");
        if (userIDStr != null && !userIDStr.isEmpty()) {
            try {
                int userId = Integer.parseInt(userIDStr);
                System.out.println("Using user ID from request parameter: " + userId);
                return userId;
            } catch (NumberFormatException e) {
                System.out.println("Invalid userID format: " + userIDStr);
                return -1;
            }
        }
        
        // Check authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                // Extract token
                String token = authHeader.substring(7); // Remove "Bearer " prefix
                System.out.println("Found JWT token in Authorization header");
                
                // Parse and verify the JWT - USING THE SAME KEY AS FRONTEND
                Jws<Claims> jws = Jwts.parserBuilder()
                        .setSigningKey(SIGNING_KEY)
                        .build()
                        .parseClaimsJws(token);
                
                // Get username and email from claims
                String username = jws.getBody().getSubject();
                String email = jws.getBody().get("email", String.class);
                
                System.out.println("JWT verified for user: " + username + " with email: " + email);
                
                // Look up user ID from the database using the email
                int userId = getUserIdFromEmail(email);
                if (userId > 0) {
                    System.out.println("Found user ID: " + userId + " for email: " + email);
                    return userId;
                } else {
                    System.out.println("Could not find user ID for email: " + email);
                    return -1;
                }
                
            } catch (JwtException e) {
                System.out.println("Invalid JWT token: " + e.getMessage());
                return -1;
            } catch (Exception e) {
                System.out.println("Error processing JWT: " + e.getMessage());
                return -1;
            }
        }
        
        System.out.println("No valid authentication found in request");
        return -1;
    }
    
    /**
     * Get user ID from email address by querying the database
     */
    private int getUserIdFromEmail(String email) {
        try {
            // Use the UserPersistence to look up the user ID
            return UserPersistence.getUserIdFromEmail(email);
        } catch (Exception e) {
            System.out.println( "Error getting user ID: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Handle GET requests - retrieve goals
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        System.out.println("GoalServlet GET request received");
        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Context Path: " + request.getContextPath());
        System.out.println("Servlet Path: " + request.getServletPath());
        System.out.println("Path Info: " + request.getPathInfo());
        
        try {
            // Extract user ID from request
            int userID = extractUserID(request);
            if (userID <= 0) {
                System.out.println("Authentication failed: userID=" + userID);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Authentication failed. Please login.\"}");
                return;
            }
            
            System.out.println("GET request for goals - User ID: " + userID);
            
            // Get goals from business layer
            List<GoalInfo> goals = GoalBusiness.getUserGoals(userID);
            
            // Send response
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            try (PrintWriter out = response.getWriter()) {
                StringBuilder jsonBuilder = new StringBuilder("[");
                
                for (int i = 0; i < goals.size(); i++) {
                    if (i > 0) {
                        jsonBuilder.append(",");
                    }
                    
                    GoalInfo goal = goals.get(i);
                    jsonBuilder.append("{")
                        .append("\"id\":\"").append(goal.getUserID()).append("\",")
                        .append("\"title\":\"").append(escapeJson(goal.getTitle())).append("\",")
                        .append("\"targetDate\":\"").append(escapeJson(goal.getDate())).append("\",")
                        .append("\"metricType\":\"").append(escapeJson(goal.getMetricType())).append("\",")
                        .append("\"targetValue\":\"").append(escapeJson(goal.getTargetValue())).append("\",")
                        .append("\"targetUnit\":\"").append(escapeJson(goal.getTargetUnit())).append("\",")
                        .append("\"frequency\":\"").append(escapeJson(goal.getFrequency())).append("\",")
                        .append("\"description\":\"").append(escapeJson(goal.getDescription())).append("\",")
                        .append("\"progress\":0,")
                        .append("\"completion\":0")
                        .append("}");
                }
                
                jsonBuilder.append("]");
                System.out.println("Sending JSON response with " + goals.size() + " goals");
                out.print(jsonBuilder.toString());
            }
        } catch (Exception e) {
            System.out.println("Error in GoalServlet doGet: " + e.getMessage());
            sendErrorResponse(response, "Internal server error: " + e.getMessage(), 
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handle POST requests - create goals
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        try {
            // Extract user ID from request
            int userID = extractUserID(request);
            if (userID <= 0) {
                sendErrorResponse(response, "User not authenticated", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            
            System.out.println("POST request to create goal - User ID: " + userID);
            
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            
            // Parse JSON
            JsonObject jsonRequest;
            try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
                jsonRequest = jsonReader.readObject();
            } catch (Exception e) {
                System.out.println("Invalid JSON format: " + e.getMessage());
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
                    sendErrorResponse(response, "Missing required fields", HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                
                // Create goal object
                GoalInfo newGoal = new GoalInfo(userID, title, date, metricType, targetValue, 
                        targetUnit, frequency, description);
                
                // Create the goal
                String result = GoalBusiness.createGoal(newGoal);
                
                // Send response
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                
                if (result.contains("success")) {
                    JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                            .add("success", true)
                            .add("message", "Goal created successfully");
                            
                    try (PrintWriter out = response.getWriter()) {
                        out.print(responseBuilder.build().toString());
                    }
                } else {
                    JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                            .add("success", false)
                            .add("message", result);
                            
                    try (PrintWriter out = response.getWriter()) {
                        out.print(responseBuilder.build().toString());
                    }
                }
                
            } catch (Exception e) {
                System.out.println("Error processing goal data: " + e.getMessage());
                sendErrorResponse(response, "Error processing goal data", HttpServletResponse.SC_BAD_REQUEST);
            }
            
        } catch (Exception e) {
            System.out.println("Error in GoalServlet doPost: " + e.getMessage());
            sendErrorResponse(response, "Internal server error: " + e.getMessage(), 
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send error response in JSON format
     */
    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) 
            throws IOException {
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
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    @Override
    public String getServletInfo() {
        return "GoalService API";
    }
}