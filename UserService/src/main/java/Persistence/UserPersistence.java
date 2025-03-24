package Persistence;

import Helper.UserInfo;
import java.sql.*;
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
            String url = "jdbc:mysql://localhost:3306/levelup?zeroDateTimeBehavior=CONVERT_TO_NULL";
            String user = "root";
            String password = "student123";
            
            LOGGER.info("Attempting to connect to database...");
            Connection con = DriverManager.getConnection(url, user, password);
            LOGGER.info("Connection established successfully.");
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
    public static String create(UserInfo newUser) {
        String result = "";
        
        try {
            Connection con = getCon();
            if (con == null) {
                LOGGER.severe("Connection is null");
                return "Database connection failed";
            }
            
            // Use PreparedStatement to prevent SQL injection
            String q = "INSERT INTO USER (USER_NAME, PASSWORD, EMAIL) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = con.prepareStatement(q)) {
                stmt.setString(1, newUser.getUsername());
                stmt.setString(2, newUser.getPassword());
                stmt.setString(3, newUser.getEmail());
                
                LOGGER.log(Level.INFO, "Creating user: {0}", newUser.getUsername());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    result = "user creation success";
                } else {
                    result = "Failed to insert user";
                }
            }
            
            con.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error: {0}", e.getMessage());
            result = "Error: " + e.getMessage();
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver Error: {0}", e.getMessage());
            result = "Error: Driver not found";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected Error: {0}", e.getMessage());
            result = "Error: Unexpected error occurred";
        }
        return result;
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
