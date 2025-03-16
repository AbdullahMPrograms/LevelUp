package Controller;

import View.Authentication;
import Model.UserModel;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Frontend Service for the Level Up! application
 */
@WebServlet(name = "FrontEndService", urlPatterns = {"/FrontEndService"})
public class FrontEndService extends HttpServlet {

    Authentication auth;
    private final String authenticationCookieName = "login_token";

    public FrontEndService() {
        auth = new Authentication();
    }

    private Map.Entry<String, String> isAuthenticated(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String token = "";
        
        System.out.println("TOKEN IS");
        try {
            for (Cookie cookie : cookies) {
                System.out.println(cookie.getName());
                if (cookie.getName().equals(authenticationCookieName)) {
                    token = cookie.getValue();
                }
            }
        } catch (Exception e) {
            // Handle case where cookies are null
        }
        
        if (!token.isEmpty()) {
            try {
                if (this.auth.verify(token).getKey()) {
                    Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>(token, this.auth.verify(token).getValue());
                    return entry;
                } else {
                    Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("", "");
                    return entry;
                }
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(FrontEndService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("", "");
        return entry;
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = isAuthenticated(request).getKey();
        String username = isAuthenticated(request).getValue();
        String pageName = request.getParameter("pageName");
        
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
            case "login":
                String email = request.getParameter("email");
                String password = request.getParameter("password");
                boolean isAuthenticated = Model.UserModel.isAuthenticated(email, password);
                
                if (isAuthenticated) {
                    username = Model.UserModel.getUsernameByEmail(email);
                    request.setAttribute("username", username);
                    token = auth.createJWT("LevelUpApp", username, 100000);

                    Cookie newCookie = new Cookie(authenticationCookieName, token);
                    response.addCookie(newCookie);
                    
                    response.sendRedirect("dashboard.html?username=" + 
                            java.net.URLEncoder.encode(username, "UTF-8"));
                } else {
                    response.sendRedirect("login.html?error=invalid");
                }
                break;
                
            case "logout":
                // Remove auth cookie
                Cookie cookie = new Cookie(authenticationCookieName, "");
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);
                
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
                
            default:
                // Redirect to login if operation not recognized
                response.sendRedirect("login.html");
                break;
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
