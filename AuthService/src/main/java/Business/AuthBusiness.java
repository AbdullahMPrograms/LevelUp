package Business;

import Persistence.AuthPersistence;
import Helper.JWTHelper;

/**
 * Business logic for authentication
 */
public class AuthBusiness {    
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
                System.out.println("Empty email or password provided");
                return new Object[] { false, null, null };
            }
            
            // Authenticate against database
            Object[] authResult = AuthPersistence.authenticateUser(email, password);
            
            if (authResult == null) {
                System.out.println("Error during authentication");
                return new Object[] { false, null, null };
            }
            
            boolean isAuthenticated = (boolean) authResult[0];
            String username = (String) authResult[1];
            
            if (isAuthenticated) {
                // Generate JWT token
                String token = JWTHelper.createToken(username, email);
                System.out.println("User authenticated successfully: " + email);
                return new Object[] { true, token, username };
            } else {
                System.out.println("Authentication failed for: " + email);
                return new Object[] { false, null, null };
            }
            
        } catch (Exception e) {
            System.out.println("Error in authentication process" + e.getMessage());
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
            
            boolean isValid = JWTHelper.verifyToken(token);
            
            if (isValid) {
                String username = JWTHelper.getUsernameFromToken(token);
                String email = JWTHelper.getEmailFromToken(token);
                return new Object[] { true, username, email };
            } else {
                return new Object[] { false, null, null };
            }
            
        } catch (Exception e) {
            System.out.println("Error verifying token" + e.getMessage());
            return new Object[] { false, null, null };
        }
    }
}
