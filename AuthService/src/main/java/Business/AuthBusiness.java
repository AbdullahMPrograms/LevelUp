package Business;

import Model.AuthModel;
import Util.JWTUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Business logic for authentication
 */
public class AuthBusiness {
    
    private static final Logger LOGGER = Logger.getLogger(AuthBusiness.class.getName());
    
    /**
     * Authenticate a user and generate JWT token
     * 
     * @param email User's email
     * @param password User's password
     * @return Object array with [isAuthenticated, token, username] or null if error
     */
    public static Object[] authenticate(String email, String password) {
        try {
            // Validate input
            if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
                LOGGER.warning("Empty email or password provided");
                return new Object[] { false, null, null };
            }
            
            // Authenticate against database
            Object[] authResult = AuthModel.authenticateUser(email, password);
            
            if (authResult == null) {
                LOGGER.warning("Error during authentication");
                return new Object[] { false, null, null };
            }
            
            boolean isAuthenticated = (boolean) authResult[0];
            String username = (String) authResult[1];
            
            if (isAuthenticated) {
                // Generate JWT token
                String token = JWTUtil.createToken(username, email);
                LOGGER.info("User authenticated successfully: " + email);
                return new Object[] { true, token, username };
            } else {
                LOGGER.info("Authentication failed for: " + email);
                return new Object[] { false, null, null };
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in authentication process", e);
            return new Object[] { false, null, null };
        }
    }
    
    /**
     * Verify if a token is valid
     * 
     * @param token JWT token to verify
     * @return Object array with [isValid, username, email] or null if error
     */
    public static Object[] verifyToken(String token) {
        try {
            // Validate input
            if (token == null || token.isEmpty()) {
                return new Object[] { false, null, null };
            }
            
            boolean isValid = JWTUtil.verifyToken(token);
            
            if (isValid) {
                String username = JWTUtil.getUsernameFromToken(token);
                String email = JWTUtil.getEmailFromToken(token);
                return new Object[] { true, username, email };
            } else {
                return new Object[] { false, null, null };
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying token", e);
            return new Object[] { false, null, null };
        }
    }
}
