package com.bank.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Сервис для валидации JWT токенов
 * Токены генерируются в authService, здесь только валидация
 */
@Component
public class JwtService {

    @Value("${jwt.secret:${JWT_SECRET:34cf09cbe5571911e487cafcd49b72946a133fe881c3de75ab2d412f226ef06f}}")
    private String secret;

    @Value("${jwt.issuer:com.bank.auth}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Парсит и валидирует JWT токен
     *
     * @param token JWT токен
     * @return Claims из токена
     * @throws io.jsonwebtoken.JwtException если токен невалидный
     */
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    /**
     * Извлекает subject (email пользователя) из токена
     *
     * @param token JWT токен
     * @return email пользователя
     */
    public String getSubject(String token) {
        return parse(token).getPayload().getSubject();
    }

    /**
     * Извлекает роли из токена
     *
     * @param token JWT токен
     * @return список ролей
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> getRoles(String token) {
        Claims claims = parse(token).getPayload();
        return (java.util.List<String>) claims.get("roles");
    }
}
