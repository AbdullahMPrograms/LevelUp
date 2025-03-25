package Persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Model for user-related database operations
 */
public class UserPersistence {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/userdb?zeroDateTimeBehavior=CONVERT_TO_NULL";
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
            System.out.println("Empty email provided to getUserIdFromEmail");
            return -1;
        }
        
        System.out.println("Looking up user ID for email: " + email);
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = getConnection();
            String query = "SELECT USER_ID FROM USER WHERE EMAIL = ?";
            System.out.println("Executing query: " + query);
            
            stmt = con.prepareStatement(query);
            stmt.setString(1, email);
            
            rs = stmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("USER_ID");
                System.out.println("Found user ID: " + userId + " for email: " + email);
                return userId;
            } else {
                System.out.println("No user found with email: " + email);
            }
        } catch (SQLException e) {
            System.out.println("Database error looking up user ID: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC driver not found: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (con != null) con.close();
            } catch (SQLException e) {
                System.out.println("Error closing database resources" + e.getMessage());
            }
        }
        
        return -1;
    }
}
