package Model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Model for user-related database operations
 */
public class UserModel {
    private static final Logger LOGGER = Logger.getLogger(UserModel.class.getName());
    private static final String DB_URL = "jdbc:mysql://localhost:3306/levelup?zeroDateTimeBehavior=CONVERT_TO_NULL";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "student123";
    
    /**
     * Get a database connection
     */
    private static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    /**
     * Gets user ID from email address
     * 
     * @param email The user's email
     * @return User ID if found, -1 otherwise
     */
    public static int getUserIdFromEmail(String email) {
        if (email == null || email.isEmpty()) {
            LOGGER.warning("Empty email provided to getUserIdFromEmail");
            return -1;
        }
        
        LOGGER.info("Looking up user ID for email: " + email);
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = getConnection();
            String query = "SELECT USER_ID FROM USER WHERE EMAIL = ?";
            LOGGER.fine("Executing query: " + query);
            
            stmt = con.prepareStatement(query);
            stmt.setString(1, email);
            
            rs = stmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("USER_ID");
                LOGGER.info("Found user ID: " + userId + " for email: " + email);
                return userId;
            } else {
                LOGGER.warning("No user found with email: " + email);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error looking up user ID: " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "JDBC driver not found: " + e.getMessage(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database resources", e);
            }
        }
        
        return -1;
    }
}
