package Model;

import Helper.UserInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Model class for user operations following MVC pattern
 */
public class UserModel {
    
    private static final Logger LOGGER = Logger.getLogger(UserModel.class.getName());
    
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
            UserInfo user = User_CRUD.read(email, password);
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
            UserInfo user = User_CRUD.read(email, "");
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
        User_CRUD userCRUD = new User_CRUD();
        return userCRUD.create(newUser);
    }
}