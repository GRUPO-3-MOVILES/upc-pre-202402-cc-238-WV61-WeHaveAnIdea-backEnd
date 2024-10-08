package com.roademics.platform.upcprep202402cc238wv61wehaveanideaapi.iam.infrastructure.tokens.jwt.services;

import com.roademics.platform.upcprep202402cc238wv61wehaveanideaapi.iam.infrastructure.tokens.jwt.BearerTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class TokenServiceImpl implements BearerTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int TOKEN_BEGIN_INDEX = 7;

    private final SecretKey key;

    @Value("${authorization.jwt.expiration.days}")
    private int expirationDays;

    // Usamos un algoritmo de generación de claves más seguro para JWT HMAC-SHA
    public TokenServiceImpl() {
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // Genera una clave de 256 bits para HMAC-SHA256
    }

    @Override
    public String getBearerTokenFrom(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (isTokenPresent(authHeader) && isBearerToken(authHeader)) {
            return extractToken(authHeader);
        }
        return null;
    }

    @Override
    public String generateToken(Authentication authentication) {
        return buildToken(authentication.getName());
    }

    @Override
    public String generateToken(String username) {
        return buildToken(username);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            LOGGER.info("JSON Web Token is valid");
            return true;
        } catch (JwtException e) {
            LOGGER.error("Invalid JSON Web Token: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private String buildToken(String username) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + expirationDays * 86400000L); // Convertimos días a milisegundos
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(key)  // Firma con la clave segura de 256 bits
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private boolean isTokenPresent(String authHeader) {
        return StringUtils.hasText(authHeader);
    }

    private boolean isBearerToken(String authHeader) {
        return authHeader.startsWith(BEARER_PREFIX);
    }

    private String extractToken(String authHeader) {
        return authHeader.substring(TOKEN_BEGIN_INDEX);
    }
}
