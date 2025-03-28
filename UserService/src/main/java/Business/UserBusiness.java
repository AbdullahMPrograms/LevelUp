package Business;

import Helper.UserInfo;
import Persistence.UserPersistence;
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
            UserInfo user = UserPersistence.read(email, password);
            return (user != null && user.getPassword().equals(password));
        } catch (Exception e) {
            System.out.println("Error during authentication: {0}" + e.getMessage());
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
            UserInfo user = UserPersistence.read(email, "");
            return (user != null) ? user.getUsername() : null;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error retrieving username for email " + email + ": {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Creates a new user with a plain text password and returns a status message.
     *
     * @param newUser User information to create (with plain text password).
     * @return Result message indicating success (with ID) or failure.
     */
    public static String createUser(UserInfo newUser) {
        // Basic Validation
        if (newUser == null || newUser.getUsername() == null || newUser.getEmail() == null || newUser.getPassword() == null ||
            newUser.getUsername().trim().isEmpty() || newUser.getEmail().trim().isEmpty() || newUser.getPassword().isEmpty()) {
             LOGGER.log(Level.WARNING, "User creation failed: Missing required user information.");
            return "Error: Missing required user information.";
        }

        // Call Persistence Layer
        LOGGER.log(Level.INFO, "Attempting to create user in persistence layer for email: {0}", newUser.getEmail());
        int newUserId = UserPersistence.create(newUser); // Call the method that returns int ID

        if (newUserId > 0) {
            // Success case
            String successMessage = "user creation success with ID: " + newUserId;
            LOGGER.log(Level.INFO, "User created successfully with ID: {0} (Email: {1})", new Object[]{newUserId, newUser.getEmail()});
            return successMessage;
        } else {
            // Failure case (newUserId is likely -1)
            String errorMessage = "Error: User creation failed. Possible duplicate email or database issue.";
            LOGGER.log(Level.WARNING, errorMessage + " (Email: {0})", newUser.getEmail());
            return errorMessage; 
        }
    }


    /**
     * Gets user by ID
     *
     * @param userId User ID
     * @return User information if found, null otherwise
     */
    public static UserInfo getUserById(int userId) {
        return UserPersistence.getUserById(userId);
    }

    /**
     * Gets user by email
     *
     * @param email User's email
     * @return User information if found, null otherwise
     */
    public static UserInfo getUserByEmail(String email) {
        return UserPersistence.read(email, "");
    }
}