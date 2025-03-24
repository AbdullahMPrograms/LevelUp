package Helper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Utility for JWT token generation and validation
 */
public class JWTHelper {

    private static final String SECRET_KEY = "abcdefghijklmnopqrstuvwxyz1234567890"; // 36 chars = 288 bits
    private static final long TOKEN_VALIDITY = 24 * 60 * 60 * 1000; // 24 hours
    
    // Create the signing key with Keys.hmacShaKeyFor (THIS IS THE ONLY WAY I COULD GET IT TO NOT COMPLAIN ABOUT KEY IS LESS THEN 256 BITS, YOU DONT KNOW HOW LONG I SPENT ON THIS)
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    /**
     * Creates a JWT token for the user
     * 
     * @param username The username to include in the token
     * @param email The user's email
     * @return The generated JWT token
     */
    public static String createToken(String username, String email) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        
        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(now)
                .setSubject(username)
                .claim("email", email)
                .setIssuer("LevelUpAuth")
                .signWith(SIGNING_KEY);

        long expMillis = nowMillis + TOKEN_VALIDITY;
        builder.setExpiration(new Date(expMillis));
        return builder.compact();
    }

    /**
     * Verifies a JWT token.
     * 
     * @param jwt The JWT token to verify
     * @return true if token is valid, false otherwise
     */
    public static boolean verifyToken(String jwt) {
        try {
            System.out.println("Verifying JWT using key: " + SECRET_KEY);
            Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(jwt);
            return true;
        } catch (JwtException ex) {
            System.out.println("JWT verification failed: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * Extracts the username from a JWT token.
     * 
     * @param jwt The JWT token
     * @return The username if token is valid, null otherwise.
     */
    public static String getUsernameFromToken(String jwt) {
        try {
            System.out.println("Verifying JWT using key: " + SECRET_KEY);
            Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(jwt);
            return jws.getBody().getSubject();
        } catch (JwtException ex) {
            System.out.println("JWT verification failed: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }
    
    /**
     * Extracts the email from a JWT token.
     * 
     * @param jwt The JWT token
     * @return The email if token is valid, null otherwise.
     */
    public static String getEmailFromToken(String jwt) {
        try {
            System.out.println("Verifying JWT using key: " + SECRET_KEY);
            Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(jwt);
            return jws.getBody().get("email", String.class);
        } catch (JwtException ex) {
            System.out.println("JWT verification failed: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }
}
