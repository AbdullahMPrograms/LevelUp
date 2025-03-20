package Business;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Map.Entry;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Authentication class for handling JWT tokens in the Frontend Service
 */
public class Authentication {

    private SignatureAlgorithm signatureAlgorithm;
    private final String SECRET_KEY = "abcdefghijklmnopqrstuvwxyz1234567890";
    private final Key SIGNING_KEY;

    public Authentication() {
        signatureAlgorithm = SignatureAlgorithm.HS256;
        // Use Keys.hmacShaKeyFor instead of Base64.getDecoder().decode
        SIGNING_KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        System.out.println("FrontendService using key: " + SECRET_KEY);
    }

    /**
     * Creates a JWT token for the user
     * 
     * @param issuer The issuer of the token (application name)
     * @param subject The subject (username)
     * @param ttlMillis Time to live in milliseconds
     * @return The generated JWT token
     */
    public String createJWT(String issuer, String subject, long ttlMillis) {
        // Current time
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        
        // Build JWT with claims
        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(now)
                .setSubject(subject)
                .setIssuer(issuer)
                .signWith(SIGNING_KEY);

        // Set expiration if ttl > 0
        if (ttlMillis > 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }

        // Build the JWT and serialize it to a compact, URL-safe string
        return builder.compact();
    }

    /**
     * Verifies a JWT token
     * 
     * @param jwt The JWT token to verify
     * @return An Entry with Boolean (valid/invalid) and String (username if valid)
     * @throws UnsupportedEncodingException
     */
    public Entry<Boolean, String> verify(String jwt) throws UnsupportedEncodingException {
        if (jwt == null || jwt.isEmpty()) {
            return new AbstractMap.SimpleEntry<>(false, "");
        }
        
        System.out.println("JWT to verify: " + jwt.substring(0, Math.min(20, jwt.length())) + "...");
        
        try {
            // Parse and verify the JWT using the SIGNING_KEY
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(SIGNING_KEY)
                    .build()
                    .parseClaimsJws(jwt);
            
            // Get username from claims
            String username = jws.getBody().getSubject();
            
            // Check if token is expired
            long nowMillis = System.currentTimeMillis();
            Date now = new Date(nowMillis);
            if (jws.getBody().getExpiration().before(now)) {
                System.out.println("JWT token expired");
                return new AbstractMap.SimpleEntry<>(false, "");
            }
            
            // Token is valid
            System.out.println("JWT verified for user: " + username);
            return new AbstractMap.SimpleEntry<>(true, username);
            
        } catch (JwtException ex) {
            // Token validation failed
            System.out.println("JWT verification failed: " + ex.getMessage());
            return new AbstractMap.SimpleEntry<>(false, "");
        }
    }
}
