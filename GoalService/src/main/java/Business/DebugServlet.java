package Business;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple debug servlet to test if the service is deployed correctly
 */
@WebServlet(name = "DebugServlet", urlPatterns = {"/api/debug", "/debug"})
public class DebugServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(DebugServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        LOGGER.info("Debug servlet GET request received");
        response.setContentType("text/html");
        
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>GoalService Debug</title></head><body>");
            out.println("<h1>GoalService Debug Page</h1>");
            
            // Basic info
            out.println("<h2>Server Info</h2>");
            out.println("<p>Context Path: " + request.getContextPath() + "</p>");
            out.println("<p>Servlet Path: " + request.getServletPath() + "</p>");
            out.println("<p>Path Info: " + request.getPathInfo() + "</p>");
            out.println("<p>Request URL: " + request.getRequestURL() + "</p>");
            
            // Headers
            out.println("<h2>Request Headers</h2>");
            out.println("<ul>");
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String header = headerNames.nextElement();
                out.println("<li>" + header + ": " + request.getHeader(header) + "</li>");
            }
            out.println("</ul>");
            
            // Check database connection
            out.println("<h2>Testing Database Connection</h2>");
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                out.println("<p style='color:green'>MySQL Driver loaded successfully</p>");
            } catch (ClassNotFoundException e) {
                out.println("<p style='color:red'>Failed to load MySQL Driver: " + e.getMessage() + "</p>");
            }
            
            out.println("</body></html>");
        } catch (Exception e) {
            LOGGER.severe("Error in debug servlet: " + e.getMessage());
            e.printStackTrace();
        }
    }
}