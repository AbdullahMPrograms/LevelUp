import Business.AuthBusiness;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;

/**
 * Servlet for authentication endpoints
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/*"})
public class AuthServlet extends HttpServlet {
    
    private static final Logger LOGGER = Logger.getLogger(AuthServlet.class.getName());
    
    /**
     * Handle login requests
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        System.out.println("Auth Service: Login request received");
        
        // Read JSON from request body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        
        System.out.println("Request body: " + sb.toString());
        
        // Parse JSON
        JsonObject jsonRequest;
        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            jsonRequest = jsonReader.readObject();
        } catch (Exception e) {
            System.out.println("Invalid JSON format: " + e.getMessage());
            sendErrorResponse(response, "Invalid JSON format");
            return;
        }
        
        // Extract email and password
        String email = jsonRequest.getString("email", "");
        String password = jsonRequest.getString("password", "");
        
        System.out.println("Authenticating: " + email);
        
        // Authenticate user
        Object[] authResult = AuthBusiness.authenticate(email, password);
        boolean isAuthenticated = (boolean) authResult[0];
        String token = (String) authResult[1];
        String username = (String) authResult[2];
        
        System.out.println("Authentication result: " + isAuthenticated);
        
        // Prepare response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        if (isAuthenticated) {
            JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                    .add("success", true)
                    .add("token", token)
                    .add("username", username);
                    
            response.setContentType("application/json");
            response.getWriter().write(responseBuilder.build().toString());
        } else {
            JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                    .add("success", false)
                    .add("message", "Invalid email or password");
                    
            response.setContentType("application/json");
            response.getWriter().write(responseBuilder.build().toString());
        }
    }
    
    /**
     * Handle token verification requests
     */
    private void handleVerify(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        // Get token from Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Remove "Bearer " prefix
        }
        
        // Verify token
        Object[] verifyResult = AuthBusiness.verifyToken(token);
        boolean isValid = (boolean) verifyResult[0];
        String username = (String) verifyResult[1];
        String email = (String) verifyResult[2];
        
        // Prepare response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        if (isValid) {
            JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                    .add("valid", true)
                    .add("username", username)
                    .add("email", email);
                    
            try (PrintWriter out = response.getWriter()) {
                out.print(responseBuilder.build().toString());
            }
        } else {
            JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                    .add("valid", false);
                    
            try (PrintWriter out = response.getWriter()) {
                out.print(responseBuilder.build().toString());
            }
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        // Route request based on path
        if (pathInfo.equals("/verify")) {
            handleVerify(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        // Route request based on path
        if (pathInfo.equals("/login")) {
            handleLogin(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public String getServletInfo() {
        return "Authentication Service API";
    }
}
