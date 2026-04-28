package com.career.planning.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * Token 有效期 24 小时，仅包含 username 唯一标识
 * 密钥从 application.yml jwt.secret 配置读取，支持通过环境变量 JWT_SECRET 注入
 */
@Component
public final class JwtUtil {

    private static final String DEFAULT_SECRET = "career-planning-a13-fuchuang-2025-secret-key-must-be-32bytes";

    private final String secret;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret:" + DEFAULT_SECRET + "}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    /**
     * 生成 JWT Token，有效期 24 小时，claims 仅包含 username
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    /**
     * 解析 Token，返回 username（subject）
     * 如果 Token 过期、无效、格式错误均返回 null
     */
    public String parseUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 校验 Token 是否有效（格式正确、未过期）
     * 注意：此方法不校验黑名单，黑名单校验请使用 JwtBlacklistUtil
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Token 剩余有效期（毫秒），已过期返回 0
     */
    public long getRemainingMs(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Math.max(0, claims.getExpiration().getTime() - System.currentTimeMillis());
        } catch (Exception e) {
            return 0;
        }
    }
}
