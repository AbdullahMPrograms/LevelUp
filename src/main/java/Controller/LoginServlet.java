package Controller;

import View.Authentication;
import Model.UserModel;
import Helper.UserInfo;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LoginServlet - Handles user authentication for the Level Up! application
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    private Authentication auth;
    private final String AUTH_COOKIE_NAME = "login_token";
    
    public LoginServlet() {
        auth = new Authentication();
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pageName = request.getParameter("pageName");
        
        // If no pageName is provided, default to "login"
        if (pageName == null) {
            pageName = "login";
        }
        
        switch (pageName) {
            case "login":
                handleLogin(request, response);
                break;
                
            case "logout":
                handleLogout(request, response);
                break;
                
            default:
                response.sendRedirect("login.html");
                break;
        }
    }
    
    /**
     * Handle user login
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        
        // Validate input
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            sendLoginError(response, "Email and password are required");
            return;
        }
        
        // Check if user is authenticated
        boolean isAuthenticated = UserModel.isAuthenticated(email, password);
        
        if (isAuthenticated) {
            // Get username (or use email if username is not available)
            String username = UserModel.getUsernameByEmail(email);
            if (username == null || username.isEmpty()) {
                username = email.split("@")[0];  // Use part before @ as username
            }
            
            // Set attributes in request
            request.setAttribute("email", email);
            request.setAttribute("username", username);
            
            // Create JWT token
            String token = auth.createJWT("LevelUpApp", username, 24 * 60 * 60 * 1000); // 24 hours
            
            // Add token as cookie
            Cookie newCookie = new Cookie(AUTH_COOKIE_NAME, token);
            newCookie.setMaxAge(24 * 60 * 60);  // 24 hours in seconds
            newCookie.setPath("/");
            response.addCookie(newCookie);
            
            // Create session
            HttpSession session = request.getSession(true);
            session.setAttribute("email", email);
            session.setAttribute("username", username);
            session.setAttribute("isLoggedIn", true);
            
            // Redirect to dashboard with username
            response.sendRedirect("dashboard.html?username=" + java.net.URLEncoder.encode(username, "UTF-8"));
        } else {
            sendLoginError(response, "Invalid email or password");
        }
    }
    
    /**
     * Handle user logout
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Delete authentication cookie
        Cookie cookie = new Cookie(AUTH_COOKIE_NAME, "");
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
    }
    
    /**
     * Send error response for login failures
     */
    private void sendLoginError(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        out.print("{\"error\": \"" + message + "\"}");
        out.flush();
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
        return "Handles user authentication for Level Up!";
    }
}
