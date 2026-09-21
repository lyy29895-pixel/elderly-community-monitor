package com.example.demo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * JWT 工具类单元测试：验证令牌签发、声明解析、空角色兼容与过期/伪造拒绝。
 * 通过反射注入 @Value 配置项，无需启动 Spring 容器。
 * */
public class JwtUtilTest {

    /** HS256 要求密钥至少 32 字节，此处使用 32 个 ASCII 字符。 */
    private static final String SECRET = "0123456789ABCDEF0123456789ABCDEF";

    private JwtUtil jwtUtil;

    @Before
    public void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expireHours", 24L);
    }

    /** 签发后可解析：subject 为用户 ID，username/role 声明一致。 */
    @Test
    public void createThenParseRoundTrip() {
        String token = jwtUtil.createToken(5, "admin1", "community");

        Claims claims = jwtUtil.parse(token);
        assertEquals("5", claims.getSubject());
        assertEquals("admin1", claims.get("username", String.class));
        assertEquals("community", claims.get("role", String.class));
    }

    /** role 为 null 时签发为空串，解析不抛错。 */
    @Test
    public void nullRoleBecomesEmptyClaim() {
        String token = jwtUtil.createToken(1, "u1", null);
        assertEquals("", jwtUtil.parse(token).get("role", String.class));
    }

    /** parse 兼容旧命名，与 parseToken 解析结果一致。 */
    @Test
    public void parseAliasMatchesParseToken() {
        String token = jwtUtil.createToken(2, "u2", "elder");
        assertEquals(jwtUtil.parse(token).getSubject(), jwtUtil.parse(token).getSubject());
    }

    /** 过期令牌解析被拒绝。 */
    @Test
    public void expiredTokenIsRejected() {
        ReflectionTestUtils.setField(jwtUtil, "expireHours", -1L);
        String token = jwtUtil.createToken(3, "u3", "child");

        try {
            jwtUtil.parse(token);
            fail("过期令牌应解析失败");
        } catch (ExpiredJwtException expected) {
            // 预期行为
        }
    }

    /** 密钥不一致（伪造/篡改）的令牌解析被拒绝。 */
    @Test
    public void forgedTokenIsRejected() {
        String token = jwtUtil.createToken(4, "u4", "admin");

        JwtUtil attacker = new JwtUtil();
        ReflectionTestUtils.setField(attacker, "secret", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        ReflectionTestUtils.setField(attacker, "expireHours", 24L);
        try {
            attacker.parse(token);
            fail("密钥不一致应解析失败");
        } catch (JwtException expected) {
            // 预期行为
        }
    }
}
