package Business;

import java.io.IOException;
import java.io.PrintWriter;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "TestServlet", urlPatterns = {"/test"})
public class TestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Auth Test</title></head><body>");
        out.println("<h1>Authentication Test</h1>");
        
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        
        if (email != null && password != null) {
            out.println("<p>Testing authentication for: " + email + "</p>");
            
            try {
                JsonObject result = ServiceClient.authenticate(email, password);
                
                if (result != null) {
                    out.println("<h3>Authentication Response:</h3>");
                    out.println("<pre>" + result.toString() + "</pre>");
                    
                    boolean success = result.getBoolean("success", false);
                    out.println("<p>Success: " + success + "</p>");
                    
                    if (success) {
                        String token = result.getString("token", "");
                        out.println("<p>Token: " + token + "</p>");
                        
                        out.println("<h3>User Info Test:</h3>");
                        JsonObject userInfo = ServiceClient.getUserInfo(email, token);
                        
                        if (userInfo != null) {
                            out.println("<pre>" + userInfo.toString() + "</pre>");
                        } else {
                            out.println("<p>Failed to get user info</p>");
                        }
                    }
                } else {
                    out.println("<p>Authentication failed - null response</p>");
                }
            } catch (Exception e) {
                out.println("<h3>Error:</h3>");
                out.println("<pre>" + e.getMessage() + "</pre>");
                e.printStackTrace(out);
            }
        } else {
            out.println("<p>Please provide email and password parameters</p>");
            out.println("<form>");
            out.println("Email: <input name='email' type='email'><br>");
            out.println("Password: <input name='password' type='password'><br>");
            out.println("<button type='submit'>Test</button>");
            out.println("</form>");
        }
        
        out.println("</body></html>");
    }
}
