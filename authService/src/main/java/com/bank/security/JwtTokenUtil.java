package com.bank.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.Serializable;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenUtil implements Serializable {

    @Value("${app.jwt.secret}")
    private String secret;

    private final JwtTokenProvider jwtTokenProvider;

    public Long getUserIdFromToken(String token) {
        try {
            final Claims claims = getAllClaimsFromToken(token);
            return claims.get("id", Long.class);
        } catch (Exception e) {
            log.warn("Failed to get user id from token: {}", e.getMessage());
            return null;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            final Claims claims = getAllClaimsFromToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.warn("Failed to get username from token: {}", e.getMessage());
            return null;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            final Claims claims = getAllClaimsFromToken(token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            log.warn("Failed to get email from token: {}", e.getMessage());
            return null;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, MyUserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return username != null && username.equals(userDetails.getUsername());
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.warn("Token expiration check failed: {}", e.getMessage());
            return true;
        }
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }
}
