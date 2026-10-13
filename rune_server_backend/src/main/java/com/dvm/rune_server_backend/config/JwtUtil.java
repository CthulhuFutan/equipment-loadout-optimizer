package com.dvm.rune_server_backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @author 25826
 * 作用：
 * 1. generateToken(userId, username)：生成 JWT 字符串
 * 2. getUserIdFromToken(token)：从令牌中解析 userId
 * 3. validateToken(token)：验证令牌是否有效（签名正确 + 未过期）
 * 4. parseToken(token)：内部方法，解析并验证签名
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }
    // 1.生成 JWT 字符串
    public String generateToken(Long userId, String username) {
        Date date = new Date();
        return Jwts.builder()
                // 谁持有这个令牌
                .subject(userId.toString())
                // 额外附带的信息，用于放入token，减少查询数据库的次数。
                // 注：无加密效果，不可放密码等敏感信息
                .claim("username", username)
                // 签发时间
                .issuedAt(date)
                // 过期时间
                .expiration(new Date(date.getTime() + expiration))
                // HMAC-SHA256 签名，辅助设置header
                .signWith(key)
                // 拼接字符串
                .compact();
    }

    // 2.从令牌中解析 userId
    public Long getUserIdFromToken(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    // 3.验证令牌是否有效（签名正确 + 未过期）
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 签名不对、已过期、格式错误等，统统视为无效
            return false;
        }
    }

    // 4.内部方法，解析并验证签名
    private Claims parseToken(String token) {
        return Jwts.parser()
                // 告知使用什么密钥验证签名
                .verifyWith(key)
                // 冻结当前配置，根据已获得配置信息解析
                .build()
                // 解析并验证
                .parseSignedClaims(token)
                // 获取payload。使用者可以通过claims.get方法获取需要的信息
                .getPayload();
    }
}
