package Persistence;

import Helper.UserInfo;
import java.sql.*;
import java.io.IOException; // Keep this import if Business.Messaging is used
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access model for user operations
 */
public class UserPersistence {
    private static final Logger LOGGER = Logger.getLogger(UserPersistence.class.getName());

    // Environment variable names for database connection
    private static final String DB_HOST_ENV = "USERDB_HOST";
    private static final String DB_PORT_ENV = "USERDB_PORT";
    private static final String DB_NAME_ENV = "USERDB_NAME";
    private static final String DB_USER_ENV = "USERDB_USER";
    private static final String DB_PASS_ENV = "USERDB_PASS"; // Be cautious with passwords in env vars

    // Default values for local testing
    private static final String DEFAULT_DB_HOST = "localhost";
    private static final String DEFAULT_DB_PORT = "3306";
    private static final String DEFAULT_DB_NAME = "userdb";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASS = "student123"; // Your local default password

    /**
     * Get database connection using environment variables with local defaults.
     */
    private static Connection getCon() throws SQLException, ClassNotFoundException {
        String dbHost = System.getenv(DB_HOST_ENV) != null ? System.getenv(DB_HOST_ENV) : DEFAULT_DB_HOST;
        String dbPort = System.getenv(DB_PORT_ENV) != null ? System.getenv(DB_PORT_ENV) : DEFAULT_DB_PORT;
        String dbName = System.getenv(DB_NAME_ENV) != null ? System.getenv(DB_NAME_ENV) : DEFAULT_DB_NAME;
        String dbUser = System.getenv(DB_USER_ENV) != null ? System.getenv(DB_USER_ENV) : DEFAULT_DB_USER;
        String dbPass = System.getenv(DB_PASS_ENV) != null ? System.getenv(DB_PASS_ENV) : DEFAULT_DB_PASS;

        // Construct JDBC URL dynamically
        // Added common parameters useful for cloud environments
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

    /**
     * Read user information by email.
     * Note: The password parameter is kept for signature compatibility but is not used for lookup.
     * Authentication should happen separately.
     */
    public static UserInfo read(String email, String password){ // Password parameter kept but unused
        UserInfo bean = null;
        Connection con = null; // Declare connection here
        PreparedStatement stmt = null; // Declare statement here
        ResultSet rs = null; // Declare result set here

        try{
            con = getCon(); // Get connection using the updated method

            String q = "Select * FROM `user` WHERE EMAIL = ?"; // Fetch user by email
            stmt = con.prepareStatement(q);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if(rs.next()){
                bean = new UserInfo();
                bean.setUserID(rs.getInt("USER_ID"));
                bean.setUsername(rs.getString("USER_NAME"));
                bean.setPassword(rs.getString("PASSWORD")); // Still retrieving plain text password for auth check elsewhere
                bean.setEmail(rs.getString("EMAIL"));
                 LOGGER.log(Level.INFO, "User found in DB for email: {0}", email);
            } else {
                 LOGGER.log(Level.WARNING, "User not found in DB for email: {0}", email);
            }
        }
        catch(Exception e){
             LOGGER.log(Level.SEVERE, "Error reading user for email " + email + ": {0}", e.getMessage());
        } finally {
             // Close resources in finally block
             try { if (rs != null) rs.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing ResultSet", e);}
             try { if (stmt != null) stmt.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing PreparedStatement", e);}
             try { if (con != null) con.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing Connection", e);}
        }
        return bean;
    }

    /**
     * Create a new user
     */
    public static int create(UserInfo newUser) {
        int newUserId = -1;
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;

        try {
            con = getCon(); // Assume this works based on previous logs
            LOGGER.log(Level.INFO, "Creating user: {0}", newUser.getUsername());
    
            // ********** DEBUGGING POINT 1: Log the exact query **********
            // Make sure this query uses the table name case matching your LATEST userdb.sql
            // If userdb.sql uses CREATE TABLE `USER`, this should be USER.
            // If userdb.sql uses CREATE TABLE `user`, this should be user.
            // Let's assume you tried uppercase USER most recently:
            String q = "INSERT INTO `user` (USER_NAME, PASSWORD, EMAIL) VALUES (?, ?, ?)";
            LOGGER.log(Level.INFO, "[DEBUG] Executing SQL: {0}", q);
            // ***********************************************************
    
            stmt = con.prepareStatement(q, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, newUser.getUsername());
            stmt.setString(2, newUser.getPassword());
            stmt.setString(3, newUser.getEmail());
    
            // ********** DEBUGGING POINT 2: Check connection validity **********
            boolean isConnectionValid = false;
            try {
                 isConnectionValid = con.isValid(2); // Check validity with a 2-second timeout
                 LOGGER.log(Level.INFO, "[DEBUG] Connection valid before executeUpdate: {0}", isConnectionValid);
            } catch (SQLException validationEx) {
                 LOGGER.log(Level.WARNING,"[DEBUG] Error checking connection validity: " + validationEx.getMessage());
            }
            if (!isConnectionValid) {
                 LOGGER.log(Level.SEVERE, "[DEBUG] Connection is NOT valid right before executeUpdate!");
                 // Optionally attempt reconnect or throw specific exception
                 // For now, just log and let the executeUpdate fail as it would
            }
            // **************************************************************
    
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    newUserId = generatedKeys.getInt(1);
                    LOGGER.log(Level.INFO, "User created successfully with ID: {0}", newUserId);
                     try {
                         String message = String.format("CREATED:%d:%s:%s",
                                 newUserId,
                                 newUser.getUsername(),
                                 newUser.getEmail());
                          LOGGER.log(Level.INFO, "Attempting to send KubeMQ message for new user ID: {0}", newUserId);
                         Business.Messaging.sendMessage(message);
                     } catch (Exception e) { // Catch broader exception if Messaging throws something other than IOException
                          LOGGER.log(Level.SEVERE, "Failed to send KubeMQ message for new user {0}: {1}", new Object[]{newUserId, e.getMessage()});
                     }
                } else {
                     LOGGER.log(Level.SEVERE, "Failed to retrieve generated key for new user.");
                }
            } else {
                 LOGGER.log(Level.WARNING, "User creation failed - no rows affected.");
            }
        } catch (Exception e) { // Catch broader Exception for getCon() issues too
             LOGGER.log(Level.SEVERE, "Error creating user ("+e.getClass().getName()+"): {0}", e.getMessage());
        } finally {
             // Close resources in finally block
             try { if (generatedKeys != null) generatedKeys.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing GeneratedKeys", e);}
             try { if (stmt != null) stmt.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing PreparedStatement", e);}
             try { if (con != null) con.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing Connection", e);}
        }
        return newUserId;
    }

    /**
     * Get user by ID (excluding password)
     */
    public static UserInfo getUserById(int userId) {
        UserInfo user = null;
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = getCon();
            String query = "SELECT USER_ID, USER_NAME, EMAIL FROM `user` WHERE USER_ID = ?"; 
            stmt = con.prepareStatement(query);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                user = new UserInfo();
                user.setUserID(rs.getInt("USER_ID"));
                user.setUsername(rs.getString("USER_NAME"));
                user.setEmail(rs.getString("EMAIL"));
                user.setPassword("");
                 LOGGER.log(Level.INFO, "User found by ID: {0}", userId);
            } else {
                 LOGGER.log(Level.WARNING, "User not found by ID: {0}", userId);
            }
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error getting user by ID {0}: {1}", new Object[]{userId, e.getMessage()});
        } finally {
             // Close resources in finally block
             try { if (rs != null) rs.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing ResultSet", e);}
             try { if (stmt != null) stmt.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing PreparedStatement", e);}
             try { if (con != null) con.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing Connection", e);}
        }
        return user;
    }
}
