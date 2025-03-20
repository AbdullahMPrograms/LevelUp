package Business;

import Helper.UserInfo;
import Model.UserModel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Business logic for user operations
 */
public class UserBusiness {
    
    private static final Logger LOGGER = Logger.getLogger(UserBusiness.class.getName());
    
    /**
     * Verifies if a user is authenticated based on email and password
     * 
     * @param email User's email
     * @param password User's password
     * @return true if authentication succeeds, false otherwise
     */
    public static boolean isAuthenticated(String email, String password) {
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            return false;
        }
        
        try {
            UserInfo user = UserModel.read(email, password);
            return (user != null && user.getPassword().equals(password));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during authentication: {0}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the username associated with an email address
     * 
     * @param email User's email address
     * @return username if found, null otherwise
     */
    public static String getUsernameByEmail(String email) {
        try {
            UserInfo user = UserModel.read(email, "");
            return (user != null) ? user.getUsername() : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving username: {0}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates a new user
     * 
     * @param newUser User information to create
     * @return Result message of the creation attempt
     */
    public static String createUser(UserInfo newUser) {
        return UserModel.create(newUser);
    }
    
    /**
     * Gets user by ID
     * 
     * @param userId User ID
     * @return User information if found, null otherwise
     */
    public static UserInfo getUserById(int userId) {
        return UserModel.getUserById(userId);
    }
    
    /**
     * Gets user by email
     * 
     * @param email User's email
     * @return User information if found, null otherwise
     */
    public static UserInfo getUserByEmail(String email) {
        return UserModel.read(email, "");
    }
}
