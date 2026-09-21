package com.example.demo.auth;

import com.example.demo.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 登录认证拦截器单元测试：覆盖预检放行、缺失/非法 Token 拒绝、
 * 登录态比对与上下文写入、异常兜底 401 等分支。
 *
 * @author 甲
 */
public class AuthInterceptorTest {

    private JwtUtil jwtUtil;
    private TokenStoreService tokenStoreService;
    private AuthInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setUp() {
        jwtUtil = Mockito.mock(JwtUtil.class);
        tokenStoreService = Mockito.mock(TokenStoreService.class);
        interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(interceptor, "tokenStoreService", tokenStoreService);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        AuthContext.clear();
    }

    @After
    public void tearDown() {
        AuthContext.clear();
    }

    /** CORS 预检请求（OPTIONS）无需 Token 直接放行。 */
    @Test
    public void optionsRequestPassesThrough() {
        request.setMethod("OPTIONS");
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    /** 缺失 Authorization 头返回 401。 */
    @Test
    public void missingHeaderReturns401() {
        request.setMethod("GET");
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    /** 非 Bearer 前缀的 Authorization 头返回 401。 */
    @Test
    public void nonBearerHeaderReturns401() {
        request.setMethod("GET");
        request.addHeader("Authorization", "Basic abc123");
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    /** Token 合法且与服务端登录态一致：放行并写入上下文（角色归一化）。 */
    @Test
    public void validTokenWritesContextAndPasses() {
        request.setMethod("GET");
        request.addHeader("Authorization", "Bearer good-token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(claims.get("role", String.class)).thenReturn("COMMUNITY");
        when(jwtUtil.parse("good-token")).thenReturn(claims);
        when(tokenStoreService.matches(5, "good-token")).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(Integer.valueOf(5), AuthContext.getUserId());
        assertEquals("community", AuthContext.getRole());
    }

    /** Token 签名合法但与服务端登录态不一致（已被顶替/注销）返回 401。 */
    @Test
    public void tokenNotInStoreReturns401() {
        request.setMethod("GET");
        request.addHeader("Authorization", "Bearer stale-token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(jwtUtil.parse("stale-token")).thenReturn(claims);
        when(tokenStoreService.matches(5, "stale-token")).thenReturn(false);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertNull("认证失败不得写入上下文", AuthContext.getUserId());
    }

    /** Token 解析失败（伪造/过期）统一兜底 401。 */
    @Test
    public void parseFailureReturns401() {
        request.setMethod("GET");
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtUtil.parse("bad-token")).thenThrow(new RuntimeException("invalid jwt"));

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    /** 请求完成后清理线程上下文，防止线程池复用串数据。 */
    @Test
    public void afterCompletionClearsContext() {
        AuthContext.set(9, "elder");
        interceptor.afterCompletion(request, response, new Object(), null);
        assertNull(AuthContext.getUserId());
        assertNull(AuthContext.getRole());
    }
}
