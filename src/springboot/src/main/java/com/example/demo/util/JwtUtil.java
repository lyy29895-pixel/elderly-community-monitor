package com.example.demo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * JWT 工具类：生成与解析登录令牌（HS256）。密钥长度需满足 JJWT 要求（建议 UTF-8 下至少 32 字节）。
 * 配置项：{@code app.jwt.secret}、{@code app.jwt.expire-hours}
 *
 * @author 甲
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-hours:24}")
    private long expireHours;

    /** 依据配置密钥构建 HMAC-SHA 签名键。 */
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发访问令牌
     *
     * @param userId   用户 ID，作为令牌 subject
     * @param username 用户名，写入自定义声明
     * @param role     用户角色，写入自定义声明；为 null 时写入空串
     * @return 签名并压缩后的 JWT 字符串
     */
    public String createToken(Integer userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role == null ? "" : role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expireHours, ChronoUnit.HOURS)))
                .signWith(signingKey())
                .compact();
    }

    /**
     * 解析并校验签名、过期时间
     *
     * @param token 待解析的 JWT 字符串
     * @return 令牌中包含的全部声明
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 兼容旧调用方命名
     *
     * @param token 待解析的 JWT 字符串
     * @return 令牌中包含的全部声明
     */
    public Claims parse(String token) {
        return parseToken(token);
    }
}
