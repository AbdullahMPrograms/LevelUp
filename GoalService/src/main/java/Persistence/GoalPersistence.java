package Persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import Helper.GoalInfo;

public class GoalPersistence {
    private static final Logger LOGGER = Logger.getLogger(GoalPersistence.class.getName());
    private static final String DB_URL = "jdbc:mysql://localhost:3306/levelup?zeroDateTimeBehavior=CONVERT_TO_NULL";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "student123";

    private static Connection getConnection() throws SQLException, ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            LOGGER.info("Attempting to connect to database...");
            Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            LOGGER.info("Connection established successfully");
            return con;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Connection Error: " + e.getMessage(), e);
            throw e;
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver Error: " + e.getMessage(), e);
            throw e;
        }
    }

    public static GoalInfo read(){
        GoalInfo bean = null;


        
        return bean;
    }

    public static String create(GoalInfo newGoal) {
        String result = "";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            if (conn == null) {
                LOGGER.severe("Connection is null");
                return "Database connection failed";
            }
            
            // Use PreparedStatement to prevent SQL injection
            String query = "INSERT INTO GOAL (USER_ID, GOAL_TITLE, TARGET_DATE, METRIC_TYPE, TARGET_VALUE, TARGET_UNIT, FREQUENCY, DESCRIPTION) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, newGoal.getUserID());
            stmt.setString(2, newGoal.getTitle());
            stmt.setString(3, newGoal.getDate());
            stmt.setString(4, newGoal.getMetricType());
            stmt.setString(5, newGoal.getTargetValue());
            stmt.setString(6, newGoal.getTargetUnit());
            stmt.setString(7, newGoal.getFrequency());
            stmt.setString(8, newGoal.getDescription());
                
            LOGGER.info("Executing query: " + query);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                result = "goal creation success";
                LOGGER.info("Goal created successfully");
            } else {
                result = "Failed to insert goal";
                LOGGER.warning("No rows affected when creating goal");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error: " + e.getMessage(), e);
            result = "Error: " + e.getMessage();
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver Error: " + e.getMessage(), e);
            result = "Error: Driver not found";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected Error: " + e.getMessage(), e);
            result = "Error: Unexpected error occurred";
        } finally {
            // Close resources
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database resources", e);
            }
        }
        return result;
    }

    public List<GoalInfo> getAllUserGoals(int userID) {
        List<GoalInfo> goals = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            LOGGER.info("Database connection established");
            
            String query = "SELECT * FROM GOAL WHERE USER_ID = ?";
            LOGGER.info("Executing query: " + query + " with userID: " + userID);
            
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, userID);
            
            rs = stmt.executeQuery();
            LOGGER.info("Query executed successfully");
            
            while (rs.next()) {
                GoalInfo goal = new GoalInfo();
                goal.setUserID(rs.getInt("USER_ID"));
                goal.setTitle(rs.getString("GOAL_TITLE"));
                goal.setDate(rs.getString("TARGET_DATE"));
                goal.setMetricType(rs.getString("METRIC_TYPE"));
                goal.setTargetValue(rs.getString("TARGET_VALUE"));
                goal.setTargetUnit(rs.getString("TARGET_UNIT"));
                goal.setFrequency(rs.getString("FREQUENCY"));
                goal.setDescription(rs.getString("DESCRIPTION"));
                
                goals.add(goal);
                LOGGER.info("Added goal: " + goal.getTitle());
            }
            LOGGER.info("Total goals found: " + goals.size());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in getAllUserGoals: " + e.getMessage(), e);
        } finally {
            // Close resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database resources", e);
            }
        }
        
        return goals;
    }
}