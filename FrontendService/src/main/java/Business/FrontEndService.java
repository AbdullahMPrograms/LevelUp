package Business;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Frontend Service for the Level Up! application
 */
@WebServlet(name = "FrontEndService", urlPatterns = {"/FrontEndService"})
public class FrontEndService extends HttpServlet {

    Authentication auth;
    private final String authenticationCookieName = "login_token";
    private static final Logger LOGGER = Logger.getLogger(FrontEndService.class.getName());

    public FrontEndService() {
        auth = new Authentication();
        LOGGER.info("FrontEndService initialized with cookie name: " + authenticationCookieName);
    }

    private Map.Entry<String, String> isAuthenticated(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String token = "";
        
        LOGGER.info("Checking for authentication token");
        try {
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(authenticationCookieName)) {
                        token = cookie.getValue();
                        LOGGER.fine("Found login token cookie");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error checking cookies: " + e.getMessage());
        }
        
        if (!token.isEmpty()) {
            try {
                // Call verify only once and store the result
                Map.Entry<Boolean, String> verifyResult = auth.verify(token);
                
                if (verifyResult.getKey()) {
                    return new AbstractMap.SimpleEntry<>(token, verifyResult.getValue());
                }
            } catch (UnsupportedEncodingException ex) {
                LOGGER.log(Level.SEVERE, "Error verifying token", ex);
            }
        }

        // Return empty values if no valid token found
        return new AbstractMap.SimpleEntry<>("", "");
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        System.out.println("\n\n======= FrontEndService Request =======");
        String pageName = request.getParameter("pageName");
        System.out.println("Page requested: " + pageName);
        
        if ("login".equals(pageName)) {
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            
            System.out.println("Login attempt - Email: " + email);
            
            // Call Auth Service
            JsonObject authResult = ServiceClient.authenticate(email, password);
            System.out.println("Auth result received: " + (authResult != null ? authResult.toString() : "null"));
            
            if (authResult != null) {
                // IMPORTANT: Check the actual structure of the response
                boolean success = authResult.getBoolean("success", false);
                
                if (success) {
                    // Authentication successful
                    String token = authResult.getString("token", "");
                    
                    // Get user info
                    JsonObject userInfo = ServiceClient.getUserInfo(email, token);
                    System.out.println("User info: " + (userInfo != null ? userInfo.toString() : "null"));
                    
                    if (userInfo != null) {
                        // Create session
                        HttpSession session = request.getSession();
                        session.setAttribute("email", email);
                        session.setAttribute("username", userInfo.getString("username", email.split("@")[0]));
                        
                        // Set authentication cookie
                        Cookie cookie = new Cookie("login_token", token);
                        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
                        cookie.setPath("/");
                        response.addCookie(cookie);
                        
                        // Check for redirect parameter
                        String redirect = request.getParameter("redirect");
                        if (redirect != null && !redirect.isEmpty()) {
                            response.sendRedirect(redirect);
                        } else {
                            response.sendRedirect("dashboard.html");
                        }
                    } else {
                        System.out.println("Failed to get user info");
                        response.sendRedirect("login.html?error=invalid");
                    }
                } else {
                    // Auth failed - get error message
                    String message = authResult.getString("message", "Authentication failed");
                    System.out.println("Auth failed: " + message);
                    response.sendRedirect("login.html?error=invalid");
                }
            } else {
                System.out.println("Auth service returned null");
                response.sendRedirect("login.html?error=invalid");
            }
        } else {
            String token = isAuthenticated(request).getKey();
            String username = isAuthenticated(request).getValue();
            
            if (pageName == null) {
                // Default to dashboard if user is logged in, otherwise login page
                if (!token.isEmpty()) {
                    response.sendRedirect("dashboard.html?username=" + java.net.URLEncoder.encode(username, "UTF-8"));
                } else {
                    response.sendRedirect("login.html");
                }
                return;
            }
            
            switch (pageName) {
                case "logout":
                    // Remove auth cookie
                    Cookie cookie = new Cookie(authenticationCookieName, "");
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                    
                    // Invalidate session
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    
                    // Redirect to login page
                    response.sendRedirect("login.html");
                    break;
                    
                case "dashboard":
                    if (token.isEmpty()) {
                        response.sendRedirect("login.html");
                    } else {
                        response.sendRedirect("dashboard.html?username=" + 
                                java.net.URLEncoder.encode(username, "UTF-8"));
                    }
                    break;
                    
                case "goals":
                    // Extract goal info from the form
                    String title = request.getParameter("title");
                    String date = request.getParameter("date");
                    String metricType = request.getParameter("metricType");
                    String targetValue = request.getParameter("targetValue");
                    String targetUnit = request.getParameter("metricUnit");
                    String frequency = request.getParameter("frequency");
                    String description = request.getParameter("description");
                    
                    // Create a goal info object
                    JsonObject userGoal = Json.createObjectBuilder()
                            .add("title", title)
                            .add("date", date)
                            .add("metricType", metricType)
                            .add("targetValue", targetValue)
                            .add("targetUnit", targetUnit)
                            .add("frequency", frequency)
                            .add("description", description)
                            .build();
                    
                    // Call Goal Service to create a goal
                    boolean goalSuccess = ServiceClient.createGoal(userGoal);
                    
                    if (goalSuccess) {
                        // Goal created successfully
                        System.out.println("Goal created successfully");
                        response.sendRedirect("goals.html");
                    } else {
                        // Goal creation failed
                        System.out.println("Goal creation failure");
                        response.sendRedirect("goals.html?error=creation_failed");
                    }
                    break;
                    
                case "schedule":
                    if (token.isEmpty()) {
                        response.sendRedirect("login.html");
                    } else {
                        response.sendRedirect("schedule.html?username=" + 
                                java.net.URLEncoder.encode(username, "UTF-8"));
                    }
                    break;
                    
                case "leaderboard":
                    if (token.isEmpty()) {
                        response.sendRedirect("login.html");
                    } else {
                        response.sendRedirect("leaderboard.html?username=" + 
                                java.net.URLEncoder.encode(username, "UTF-8"));
                    }
                    break;
                    
                case "signup":
                    String signupUsername = request.getParameter("username");
                    String email = request.getParameter("email");
                    String password = request.getParameter("password");
                    String confirmPassword = request.getParameter("confirmPassword");
                    
                    System.out.println("Signup attempt - Username: " + username + ", Email: " + email);
                    
                    // Validate input
                    if (password == null || !password.equals(confirmPassword)) {
                        response.sendRedirect("signup.html?error=passwordMismatch");
                        return;
                    }
                    
                    // Create user info object
                    JsonObject userRequest = Json.createObjectBuilder()
                            .add("username", signupUsername)
                            .add("email", email)
                            .add("password", password)
                            .build();
                    
                    // Call User Service to create user
                    boolean success = ServiceClient.createUser(userRequest);
                    
                    if (success) {
                        // User created successfully, redirect to login
                        System.out.println("User created successfully: " + signupUsername);
                        response.sendRedirect("login.html?registered=true");
                    } else {
                        // User creation failed
                        System.out.println("User creation failed");
                        response.sendRedirect("signup.html?error=userExists");
                    }
                    break;
                    
                case "getGoals":
                    System.out.println("========== getGoals endpoint called ==========");
                    
                    // Try to get token from URL parameter first
                    String tokenParam = request.getParameter("token");
                    String userFromToken = "";
                    
                    if (tokenParam != null && !tokenParam.isEmpty()) {
                        System.out.println("Found token in URL parameter");
                        token = tokenParam;
                        try {
                            Map.Entry<Boolean, String> verifyResult = auth.verify(token);
                            if (verifyResult.getKey()) {
                                userFromToken = verifyResult.getValue();
                            } else {
                                token = "";
                            }
                        } catch (Exception e) {
                            token = "";
                        }
                    } else {
                        // Fall back to cookie check
                        Map.Entry<String, String> authResult = isAuthenticated(request);
                        token = authResult.getKey();
                        userFromToken = authResult.getValue();
                    }
                    
                    System.out.println("Authentication check - Token empty: " + token.isEmpty());
                    System.out.println("Username: " + userFromToken);
                    
                    if (token.isEmpty()) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"error\": \"User not authenticated\"}");
                        return;
                    }
                    
                    // Forward the request to GoalService
                    System.out.println("Calling ServiceClient.getUserGoals with token");
                    String goalJson = ServiceClient.getUserGoals(token);
                    
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    System.out.println("Returning goals JSON to client: " + goalJson);
                    response.getWriter().write(goalJson);
                    break;

                case "checkAuth":
                    Map.Entry<String, String> authCheckResult = isAuthenticated(request);
                    String authToken = authCheckResult.getKey();
                    String authUsername = authCheckResult.getValue();
                    
                    response.setContentType("application/json");
                    JsonObjectBuilder authCheckBuilder = Json.createObjectBuilder()
                            .add("authenticated", !authToken.isEmpty())
                            .add("username", authUsername);
                    
                    response.getWriter().write(authCheckBuilder.build().toString());
                    break;

                default:
                    // Redirect to login if operation not recognized
                    response.sendRedirect("login.html");
                    break;
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Frontend Service for Level Up! Fitness App";
    }
}
