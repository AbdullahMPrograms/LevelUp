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
    private static final String DB_URL = "jdbc:mysql://localhost:3306/goaldb?zeroDateTimeBehavior=CONVERT_TO_NULL";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "student123";

    private static final Logger LOGGER = Logger.getLogger(GoalPersistence.class.getName());

    private static Connection getConnection() throws SQLException, ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            return con;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Connection Error: {0}", e.getMessage());
            throw e;
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Database Driver Error: {0}", e.getMessage());
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
                System.out.println("Connection is null");
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
                
            System.out.println("Executing query: " + query);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                result = "goal creation success";
                System.out.println("Goal created successfully");
            } else {
                result = "Failed to insert goal";
                System.out.println("No rows affected when creating goal");
            }
            
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            result = "Error: " + e.getMessage();
        } catch (ClassNotFoundException e) {
            System.out.println("Driver Error: " + e.getMessage());
            result = "Error: Driver not found";
        } catch (Exception e) {
            System.out.println("Unexpected Error: " + e.getMessage());
            result = "Error: Unexpected error occurred";
        } finally {
            // Close resources
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error closing database resources" + e.getMessage());
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
            System.out.println("Database connection established");
            
            String query = "SELECT * FROM GOAL WHERE USER_ID = ?";
            System.out.println("Executing query: " + query + " with userID: " + userID);
            
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, userID);
            
            rs = stmt.executeQuery();
            System.out.println("Query executed successfully");
            
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
                System.out.println("Added goal: " + goal.getTitle());
            }
            System.out.println("Total goals found: " + goals.size());
            
        } catch (Exception e) {
            System.out.println("Error in getAllUserGoals: " + e.getMessage());
        } finally {
            // Close resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error closing database resources" + e.getMessage());
            }
        }
        
        return goals;
    }

    /**
     * Retrieves the USER_ID from the local cache based on the email address.
     *
     * @param email The email address to search for.
     * @return The USER_ID if found, otherwise -1.
     */
    public static int getUserIdFromCacheByEmail(String email) {
        int userId = -1; // Default to -1 (not found)
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        LOGGER.log(Level.INFO, "Querying USER_INFO_CACHE for email: {0}", email);

        String query = "SELECT USER_ID FROM USER_INFO_CACHE WHERE EMAIL = ?";

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, email);

            rs = stmt.executeQuery();

            if (rs.next()) {
                userId = rs.getInt("USER_ID");
                LOGGER.log(Level.INFO, "Found UserID {0} in cache for email: {1}", new Object[]{userId, email});
            } else {
                LOGGER.log(Level.WARNING, "UserID not found in cache for email: {0}", email);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error looking up user in cache for email {0}: {1}", new Object[]{email, e.getMessage()});
            // Return -1 on error
        } catch (ClassNotFoundException e) {
             LOGGER.log(Level.SEVERE, "Database Driver Error looking up user in cache: {0}", e.getMessage());
             // Return -1 on error
        } finally {
            // Close resources
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignore */ }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignore */ }
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* ignore */ }
        }

        return userId;
    }

    public static void saveUserInfoCache(int userId, String username, String email) throws SQLException, ClassNotFoundException {
        // Your existing implementation
        Connection conn = null;
        PreparedStatement stmt = null;
        LOGGER.log(Level.INFO, "Attempting to cache user info for UserID: {0}", userId);

        // Use INSERT ... ON DUPLICATE KEY UPDATE to handle both new and existing users
        String query = "INSERT INTO USER_INFO_CACHE (USER_ID, USER_NAME, EMAIL) VALUES (?, ?, ?) " +
                       "ON DUPLICATE KEY UPDATE USER_NAME = VALUES(USER_NAME), EMAIL = VALUES(EMAIL)";

        try {
            conn = getConnection(); // Use your existing getConnection method
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, email);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Successfully cached/updated user info for UserID: {0}", userId);
            } else {
                 LOGGER.log(Level.INFO, "User info for UserID: {0} already up-to-date or insert failed.", userId);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error caching user info for UserID {0}: {1}", new Object[]{userId, e.getMessage()});
            throw e; // Re-throw to be caught by the Messaging class
        } catch (ClassNotFoundException e) {
             LOGGER.log(Level.SEVERE, "Database Driver Error: {0}", e.getMessage());
             throw e; // Re-throw
        } finally {
            // Close resources
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignore */ }
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* ignore */ }
        }
    }
}