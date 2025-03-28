package Persistence;

import Helper.UserInfo;
import java.sql.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access model for user operations
 */
public class UserPersistence {    
    private static final Logger LOGGER = Logger.getLogger(UserPersistence.class.getName());
    
    /**
     * Get database connection
     */
    private static Connection getCon() throws SQLException, ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/userdb?zeroDateTimeBehavior=CONVERT_TO_NULL";
            String user = "root";
            String password = "student123";
            
            LOGGER.log(Level.INFO, "Attempting to connect to database...");
            Connection con = DriverManager.getConnection(url, user, password);
            LOGGER.log(Level.INFO, "Connection established successfully.");
            return con;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Connection Error: {0}", e.getMessage());
            throw e;
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver Error: {0}", e.getMessage());
            throw e;
        }
    }

    /**
     * Read user information by email and optionally password
     */
    public static UserInfo read(String email, String password){
        //Default to null if no user is found
        UserInfo bean = null;

        try{
            Connection con = getCon();

            String q = "Select * FROM `USER` WHERE EMAIL = ?";
            PreparedStatement stmt = con.prepareStatement(q);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            //If there is a user with the matching email
            if(rs.next()){
                bean = new UserInfo();
                int userID = rs.getInt("USER_ID");
                String userName = rs.getString("USER_NAME");
                String passWord = rs.getString("PASSWORD");
                String uemail = rs.getString("EMAIL");
                bean.setUserID(userID);
                bean.setUsername(userName);
                bean.setPassword(passWord);
                bean.setEmail(uemail);
            }
            con.close();
        }
        catch(Exception e){
            LOGGER.log(Level.SEVERE, "Error reading user: {0}", e.getMessage());
        }

        return bean;
    }
     
    /**
     * Create a new user
     */
    public static int create(UserInfo newUser) {
        int newUserId = -1; // Default to -1 if creation fails
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;

        try {
            con = getCon();
            if (con == null) {
                LOGGER.log(Level.SEVERE, "Connection is null");
                return newUserId;
            }
            
            // Use PreparedStatement to prevent SQL injection
            String q = "INSERT INTO USER (USER_NAME, PASSWORD, EMAIL) VALUES (?, ?, ?)";
            stmt = con.prepareStatement(q, Statement.RETURN_GENERATED_KEYS); // Added flag
            stmt.setString(1, newUser.getUsername());
            stmt.setString(2, newUser.getPassword()); // Store the HASHED password!
            stmt.setString(3, newUser.getEmail());
            
            LOGGER.log(Level.INFO, "Creating user: {0}", newUser.getUsername());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                // Get the generated USER_ID
                generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    newUserId = generatedKeys.getInt(1);
                    LOGGER.log(Level.INFO, "User created successfully with ID: {0}", newUserId);

                    try {
                        String message = String.format("CREATED:%d:%s:%s",
                                newUserId,
                                newUser.getUsername(),
                                newUser.getEmail());
                        Business.Messaging.sendMessage(message); // Call the messaging class
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Failed to send KubeMQ message for new user {0}: {1}", 
                                new Object[]{newUserId, e.getMessage()});
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Failed to retrieve generated key for new user.");
                }
            } else {
                LOGGER.log(Level.WARNING, "User creation failed - no rows affected.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error creating user: {0}", e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver Error: {0}", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected Error creating user: {0}", e.getMessage());
        } finally {
            // Close resources (generatedKeys, stmt, con)
            try { if (generatedKeys != null) generatedKeys.close(); } catch (SQLException e) { /* ignore */ }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignore */ }
            try { if (con != null) con.close(); } catch (SQLException e) { /* ignore */ }
        }
        return newUserId; // Return the ID (or -1 on failure)
    }
    
    /**
     * Get user by ID
     */
    public static UserInfo getUserById(int userId) {
        UserInfo user = null;
        
        try {
            Connection con = getCon();
            String query = "SELECT * FROM USER WHERE USER_ID = ?";
            
            try (PreparedStatement stmt = con.prepareStatement(query)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    user = new UserInfo();
                    user.setUserID(rs.getInt("USER_ID"));
                    user.setUsername(rs.getString("USER_NAME"));
                    user.setEmail(rs.getString("EMAIL"));
                    user.setPassword(""); // Don't return password
                }
            }
            
            con.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting user by ID: {0}", e.getMessage());
        }
        
        return user;
    }
}
