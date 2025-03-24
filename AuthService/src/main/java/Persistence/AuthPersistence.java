package Persistence;

import java.sql.*;

/**
 * Data access model for authentication
 */
public class AuthPersistence {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/levelup?zeroDateTimeBehavior=CONVERT_TO_NULL";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "student123";
    
    /**
     * Get a database connection
     * 
     * @return Connection object
     * @throws SQLException if connection fails
     * @throws ClassNotFoundException if MySQL driver not found
     */
    private static Connection getConnection() throws SQLException, ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw e;
        }
    }
    
    /**
     * Verify user credentials
     * 
     * @param email User email
     * @param password User password
     * @return Object array with [isAuthenticated, username] or null if error
     */
    public static Object[] authenticateUser(String email, String password) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            String query = "SELECT * FROM `USER` WHERE EMAIL = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            
            System.out.println("Executing query for email: " + email);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedPassword = rs.getString("PASSWORD");
                String username = rs.getString("USER_NAME");
                
                System.out.println("User found. Comparing passwords:");
                System.out.println("Stored password: " + storedPassword);
                System.out.println("Provided password: " + password);
                
                boolean isAuthenticated = storedPassword.equals(password);
                System.out.println("Password match: " + isAuthenticated);
                
                return new Object[] { isAuthenticated, username };
            } else {
                System.out.println("No user found with email: " + email);
                return new Object[] { false, null };
            }
            
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Error authenticating user: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            // Close resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Get user information by email
     * 
     * @param email User email
     * @return Object array with [userId, username, email] or null if not found
     */
    public static Object[] getUserByEmail(String email) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            String query = "SELECT * FROM `USER` WHERE EMAIL = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                int userId = rs.getInt("USER_ID");
                String username = rs.getString("USER_NAME");
                String userEmail = rs.getString("EMAIL");
                
                return new Object[] { userId, username, userEmail };
            } else {
                return null;
            }
            
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Error getting user by email" + e.getMessage());
            return null;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error closing database resources" + e.getMessage());
            }
        }
    }
}
