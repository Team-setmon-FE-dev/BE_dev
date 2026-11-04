package com.example.setmon.auth.jwt;

import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String TYPE_CLAIM = "type";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtProvider(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("JWT_SECRET 환경변수가 비어 있습니다.");
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.properties = properties;
    }

    public String createAccessToken(Long userId) {
        return create(userId, TokenType.ACCESS, properties.accessTokenValidity());
    }

    public String createRefreshToken(Long userId) {
        return create(userId, TokenType.REFRESH, properties.refreshTokenValidity());
    }

    /** 토큰을 검증하고 userId를 돌려준다. 타입이 다르면 INVALID_TOKEN. */
    public Long parseUserId(String token, TokenType expectedType) {
        Claims claims = parse(token);
        if (!expectedType.name().equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String create(Long userId, TokenType type, Duration validity) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TYPE_CLAIM, type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
