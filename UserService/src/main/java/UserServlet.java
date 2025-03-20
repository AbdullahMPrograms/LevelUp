import Business.UserBusiness;
import Helper.UserInfo;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;

/**
 * Servlet for user-related operations
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/api/users/*"})
public class UserServlet extends HttpServlet {
    
    private static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());
    
    /**
     * Handles GET requests for user data
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        String email = request.getParameter("email");
        
        // Set response type
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // User lookup by email
        if (email != null && !email.isEmpty()) {
            UserInfo user = UserBusiness.getUserByEmail(email);
            
            if (user != null) {
                JsonObjectBuilder builder = Json.createObjectBuilder()
                        .add("id", user.getUserID())
                        .add("username", user.getUsername())
                        .add("email", user.getEmail());
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(builder.build().toString());
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObjectBuilder builder = Json.createObjectBuilder()
                        .add("error", "User not found");
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(builder.build().toString());
                }
            }
            return;
        }
        
        // User lookup by ID from path
        if (pathInfo != null && !pathInfo.equals("/")) {
            try {
                int userId = Integer.parseInt(pathInfo.substring(1));
                UserInfo user = UserBusiness.getUserById(userId);
                
                if (user != null) {
                    JsonObjectBuilder builder = Json.createObjectBuilder()
                            .add("id", user.getUserID())
                            .add("username", user.getUsername())
                            .add("email", user.getEmail());
                    
                    try (PrintWriter out = response.getWriter()) {
                        out.print(builder.build().toString());
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObjectBuilder builder = Json.createObjectBuilder()
                            .add("error", "User not found");
                    
                    try (PrintWriter out = response.getWriter()) {
                        out.print(builder.build().toString());
                    }
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObjectBuilder builder = Json.createObjectBuilder()
                        .add("error", "Invalid user ID format");
                
                try (PrintWriter out = response.getWriter()) {
                    out.print(builder.build().toString());
                }
            }
        } else {
            // No user specified - this could return a list of users in a full implementation
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add("error", "User ID or email parameter required");
            
            try (PrintWriter out = response.getWriter()) {
                out.print(builder.build().toString());
            }
        }
    }
    
    /**
     * Handles POST requests for creating new users
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
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
            sendErrorResponse(response, "Invalid JSON format");
            return;
        }
        
        // Extract user data
        try {
            String username = jsonRequest.getString("username");
            String email = jsonRequest.getString("email");
            String password = jsonRequest.getString("password");
            
            // Create user object
            UserInfo newUser = new UserInfo();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setPassword(password);
            
            // Create the user
            String result = UserBusiness.createUser(newUser);
            
            // Send response
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            if (result.contains("success")) {
                JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                        .add("success", true)
                        .add("message", "User created successfully");
                        
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
            sendErrorResponse(response, "Missing required fields");
        }
    }
    
    /**
     * Send error response in JSON format
     */
    private void sendErrorResponse(HttpServletResponse response, String message) 
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                .add("success", false)
                .add("message", message);
                
        try (PrintWriter out = response.getWriter()) {
            out.print(responseBuilder.build().toString());
        }
    }

    @Override
    public String getServletInfo() {
        return "User Service API";
    }
}
