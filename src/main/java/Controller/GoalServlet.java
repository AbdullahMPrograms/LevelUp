/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.tools.ant.types.resources.selectors.Date;

import Helper.GoalInfo;
import Model.Goal_CRUD;

/**
 *
 * @author T
 */
@WebServlet(name = "GoalServlet", urlPatterns = {"/GoalServlet"})
public class GoalServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet GoalServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet GoalServlet at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            HttpSession session = request.getSession();
            if (session.getAttribute("userID") == null) {
                System.out.println("UserID is null in session");
                throw new ServletException("User not logged in");
            }
            
            int userID = Integer.parseInt(session.getAttribute("userID").toString());
            System.out.println("Fetching goals for userID: " + userID);
            
            Goal_CRUD goalCRUD = new Goal_CRUD();
            List<GoalInfo> goals = goalCRUD.getAllUserGoals(userID);
            
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
                System.out.println("Sending JSON response: " + jsonBuilder.toString());
                out.print(jsonBuilder.toString());
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("Error in GoalServlet doGet: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    // Helper method to escape JSON strings
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

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String title = request.getParameter("title");
        String date = request.getParameter("date");
        String metricType = request.getParameter("metricType");
        String targetValue = request.getParameter("targetValue");
        String targetUnit = request.getParameter("metricUnit");
        String frequency = request.getParameter("frequency");
        String description = request.getParameter("description");
        HttpSession session = request.getSession();
        int userID = Integer.parseInt(session.getAttribute("userID").toString());

        GoalInfo newGoal = new GoalInfo(userID, title, date, metricType, targetValue, targetUnit, frequency, description);
        Goal_CRUD newGoalCRUD = new Goal_CRUD();
        String result = newGoalCRUD.create(newGoal);

        if(result.equals("goal creation success")){
            response.sendRedirect("goals.html");
        }
        else{
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print("{\"goal creation error\": \"" + result + "\"}");
            out.flush();
        }

    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
