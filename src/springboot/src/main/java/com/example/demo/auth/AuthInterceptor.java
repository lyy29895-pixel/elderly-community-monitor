package com.example.demo.auth;

import com.example.demo.enums.UserRole;
import com.example.demo.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录认证拦截器：拦截受保护接口，校验请求头中的 Bearer Token，
 * 解析并比对服务端登录态后将用户信息写入 {@link AuthContext}。
 * */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private TokenStoreService tokenStoreService;

    /**
     * 请求前置处理：放行 CORS 预检请求；校验 Authorization 头中的 Bearer Token，
     * Token 合法且与服务端登录态一致时将用户 ID 与角色写入线程上下文，否则返回 401。
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler  即将执行的处理器（Controller 方法）
     * @return 放行返回 true，认证失败返回 false
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        String token = header.substring(7).trim();
        try {
            Claims claims = jwtUtil.parse(token);
            Integer userId = Integer.parseInt(claims.getSubject());
            if (!tokenStoreService.matches(userId, token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            String role = UserRole.normalize(claims.get("role", String.class));
            AuthContext.set(userId, role);
            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    /**
     * 请求完成后回调：清理线程上下文中的登录信息，防止线程复用导致数据串用。
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler  已执行的处理器（Controller 方法）
     * @param ex       处理器执行时抛出的异常，无异常时为 null
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
