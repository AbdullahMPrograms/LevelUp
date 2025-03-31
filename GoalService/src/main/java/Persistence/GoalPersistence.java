package Persistence;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import Helper.GoalInfo;

public class GoalPersistence {

    private static final Logger LOGGER = Logger.getLogger(GoalPersistence.class.getName());

    // Environment variable names for database connection
    private static final String DB_HOST_ENV = "GOALDB_HOST";
    private static final String DB_PORT_ENV = "GOALDB_PORT";
    private static final String DB_NAME_ENV = "GOALDB_NAME";
    private static final String DB_USER_ENV = "GOALDB_USER";
    private static final String DB_PASS_ENV = "GOALDB_PASS";

    // Default values for local testing
    private static final String DEFAULT_DB_HOST = "localhost";
    private static final String DEFAULT_DB_PORT = "3306";
    private static final String DEFAULT_DB_NAME = "goaldb";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASS = "student123";

    /**
     * Get database connection using environment variables with local defaults.
     */
    private static Connection getConnection() throws SQLException, ClassNotFoundException {
        String dbHost = System.getenv(DB_HOST_ENV) != null ? System.getenv(DB_HOST_ENV) : DEFAULT_DB_HOST;
        String dbPort = System.getenv(DB_PORT_ENV) != null ? System.getenv(DB_PORT_ENV) : DEFAULT_DB_PORT;
        String dbName = System.getenv(DB_NAME_ENV) != null ? System.getenv(DB_NAME_ENV) : DEFAULT_DB_NAME;
        String dbUser = System.getenv(DB_USER_ENV) != null ? System.getenv(DB_USER_ENV) : DEFAULT_DB_USER;
        String dbPass = System.getenv(DB_PASS_ENV) != null ? System.getenv(DB_PASS_ENV) : DEFAULT_DB_PASS;

        // Construct JDBC URL dynamically
        String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                                   dbHost, dbPort, dbName);

         LOGGER.log(Level.INFO, "Attempting DB connection to: {0} with user: {1}", new Object[]{url, dbUser});

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(url, dbUser, dbPass);
             LOGGER.log(Level.INFO, "DB connection established successfully.");
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
        // This method seems unused/incomplete, leaving as is for now.
        GoalInfo bean = null;
        return bean;
    }

    public static String create(GoalInfo newGoal) {
        String result = "";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection(); 
            String query = "INSERT INTO `goal` (USER_ID, GOAL_TITLE, TARGET_DATE, METRIC_TYPE, TARGET_VALUE, TARGET_UNIT, FREQUENCY, DESCRIPTION) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, newGoal.getUserID());
            stmt.setString(2, newGoal.getTitle());
            stmt.setString(3, newGoal.getDate());
            stmt.setString(4, newGoal.getMetricType());
            stmt.setString(5, newGoal.getTargetValue());
            stmt.setString(6, newGoal.getTargetUnit());
            stmt.setString(7, newGoal.getFrequency());
            stmt.setString(8, newGoal.getDescription());

            LOGGER.log(Level.INFO, "Executing goal insert for UserID: {0}, Title: {1}", new Object[]{newGoal.getUserID(), newGoal.getTitle()});

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                result = "goal creation success"; // Keep "success" for servlet check
                LOGGER.log(Level.INFO, "Goal created successfully for UserID: {0}", newGoal.getUserID());
            } else {
                result = "Failed to insert goal";
                LOGGER.log(Level.WARNING, "No rows affected when creating goal for UserID: {0}", newGoal.getUserID());
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error creating goal: {0}", e.getMessage());
            result = "Error: " + e.getMessage();
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver Error creating goal: {0}", e.getMessage());
            result = "Error: Driver not found";
        } catch (Exception e) { // Catch broader exceptions if needed
            LOGGER.log(Level.SEVERE, "Unexpected Error creating goal: {0}", e.getMessage());
            result = "Error: Unexpected error occurred";
        } finally {
            // Close resources safely
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing PreparedStatement", e); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing Connection", e); }
        }
        return result;
    }

    // Ensure getAllUserGoals uses the new getConnection and proper resource handling
    public List<GoalInfo> getAllUserGoals(int userID) {
        List<GoalInfo> goals = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        LOGGER.log(Level.INFO, "Fetching goals for UserID: {0}", userID);

        try {
            conn = getConnection(); // Use updated method
            String query = "SELECT * FROM `goal` WHERE USER_ID = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, userID);

            rs = stmt.executeQuery();
            LOGGER.log(Level.FINE, "Executed query to fetch goals for UserID: {0}", userID);

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
                LOGGER.log(Level.FINEST, "Added goal: {0} for UserID: {1}", new Object[]{goal.getTitle(), userID});
            }
            LOGGER.log(Level.INFO, "Found {0} goals for UserID: {1}", new Object[]{goals.size(), userID});

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error fetching goals for UserID {0}: {1}", new Object[]{userID, e.getMessage()});
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver Error fetching goals: {0}", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected Error fetching goals for UserID {0}: {1}", new Object[]{userID, e.getMessage()});
        } finally {
            // Close resources safely
            try { if (rs != null) rs.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing ResultSet", e); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing PreparedStatement", e); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing Connection", e); }
        }

        return goals;
    }

    /**
     * Retrieves the USER_ID from the local cache based on the email address.
     * (Assumes this method should remain static as per original code)
     *
     * @param email The email address to search for.
     * @return The USER_ID if found, otherwise -1.
     */
    public static int getUserIdFromCacheByEmail(String email) {
        int userId = -1; // Default to -1 (not found)
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        LOGGER.log(Level.INFO, "Querying user_info_cache for email: {0}", email);

        String query = "SELECT USER_ID FROM `user_info_cache` WHERE EMAIL = ?";

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
            // Close resources safely
            try { if (rs != null) rs.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing ResultSet", e); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing PreparedStatement", e); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing Connection", e); }
        }

        return userId;
    }

    // Ensure saveUserInfoCache uses the new getConnection and proper resource handling
    public static void saveUserInfoCache(int userId, String username, String email) throws SQLException, ClassNotFoundException {
        Connection conn = null;
        PreparedStatement stmt = null;
        LOGGER.log(Level.INFO, "Attempting to cache user info for UserID: {0}", userId);

        // Use INSERT ... ON DUPLICATE KEY UPDATE to handle both new and existing users
        String query = "INSERT INTO `user_info_cache` (USER_ID, USER_NAME, EMAIL) VALUES (?, ?, ?) " +
                       "ON DUPLICATE KEY UPDATE USER_NAME = VALUES(USER_NAME), EMAIL = VALUES(EMAIL)";

        try {
            conn = getConnection(); // Use updated method
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
             LOGGER.log(Level.SEVERE, "Database Driver Error caching user info: {0}", e.getMessage());
             throw e; // Re-throw
        } finally {
            // Close resources safely
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing PreparedStatement", e); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing Connection", e); }
        }
    }
}