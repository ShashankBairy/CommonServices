package com.common.config;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List; // <-- Make sure this import is present
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

    @Value("${jwt.secret:}")         // Base64 preferred; raw allowed
    private String configuredSecret;

    @Value("${jwt.exp.minutes:60}")
    private long expiryMinutes;

    private SecretKey key;
    private String base64SecretIfGenerated;

    @PostConstruct
    void init() {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
                SecretKey sk = keyGen.generateKey();
                this.key = sk;
                this.base64SecretIfGenerated = Base64.getEncoder().encodeToString(sk.getEncoded());
                System.out.println("Generated HS256 Base64 secret (dev only): " + base64SecretIfGenerated);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Unable to generate HS256 key", e);
            }
        } else {
            try {
                byte[] decoded = Base64.getDecoder().decode(configuredSecret);
                this.key = Keys.hmacShaKeyFor(decoded);
            } catch (IllegalArgumentException notBase64) {
                byte[] raw = configuredSecret.getBytes(StandardCharsets.UTF_8);
                this.key = Keys.hmacShaKeyFor(raw);
            }
        }
    }

    /** Existing method: minimal token (add your claims as needed) */
    public String generateToken(String subject, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expiryMinutes * 60);

        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString()) // jti
                .subject(subject)               // sub
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp));
        
        if (extraClaims != null && !extraClaims.isEmpty()) {
            builder.claims(extraClaims);
        }
        
        return builder.signWith(key).compact();
    }


    /** This is your core parsing method. */
    public Claims parseClaims(String jwt) {
        return Jwts.parser()
                .verifyWith(key)           // 0.12.x API
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    /** Handy helper: get exp as epoch seconds (used in your JwtResponse). */
    public long getExpiryEpochSeconds(String jwt) {
        return parseClaims(jwt).getExpiration().toInstant().getEpochSecond();
    }

    /** Optional: quick validity check against expected uid. */
    public boolean isValidFor(String jwt, int expectedUid) {
        try {
            Claims c = parseClaims(jwt);
            return String.valueOf(expectedUid).equals(c.getSubject())
                    && c.getExpiration().toInstant().isAfter(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }

    /** Dev helper if you generated a secret at runtime. */
    public Optional<String> generatedBase64Secret() {
        return Optional.ofNullable(base64SecretIfGenerated);
    }

    // --- NEW METHODS TO SUPPORT PermissionDataService ---
    // These methods use your `parseClaims` method to get the data.

    /**
     * Extracts the "name" claim from the token.
     */
    public String extractUsername(String token) {
        // The second argument tells the library to safely cast the claim to a String
        return parseClaims(token).get("name", String.class);
    }

    /**
     * Extracts the "authorized_roles" claim from the token.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        // JWT libraries decode JSON arrays into Java Lists
        return parseClaims(token).get("authorized_roles", List.class);
    }
    
    /**
     * Extracts the "authorized_designations" claim from the token.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractDesignations(String token) {
        return parseClaims(token).get("authorized_designations", List.class);
    }
}
