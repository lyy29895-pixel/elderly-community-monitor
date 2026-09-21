package com.example.demo.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.dto.LoginResponse;
import com.example.demo.entity.User;
import com.example.demo.enums.UserRole;
import com.example.demo.exception.BusinessException;
import com.example.demo.util.JwtUtil;
import com.example.demo.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 认证服务：负责用户登录校验、JWT 签发与登录态注销。
 *
 * @author 甲
 */
@Service
public class AuthService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private TokenStoreService tokenStoreService;

    /**
     * 用户登录：校验用户名与密码，拒绝已下线的家庭端角色，
     * 校验通过后签发 JWT 并保存登录态，返回不含密码的用户信息。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 包含 JWT 令牌与用户信息的登录响应
     * @throws BusinessException 用户名为空、账号不存在、密码错误或角色已下线时抛出
     */
    public LoginResponse login(String username, String password) {
        if (username == null || username.isEmpty()) {
            throw new BusinessException("401", "用户名或密码错误");
        }
        User row = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username));
        if (row == null || row.getPassword() == null || !row.getPassword().equals(password)) {
            throw new BusinessException("401", "用户名或密码错误");
        }
        if ("FAMILY".equalsIgnoreCase(row.getRole())) {
            throw new BusinessException("403", "家庭端已下线，请使用社区端、老人端、子女端或管理员账号登录");
        }
        row.setRole(UserRole.normalize(row.getRole()));
        String token = jwtUtil.createToken(row.getUserId(), row.getUsername(), row.getRole());
        tokenStoreService.saveToken(row.getUserId(), token);
        // 不再二次查询：避免多余 DB 往返；清空密码后直接返回当前行即可
        row.setPassword(null);
        LoginResponse res = new LoginResponse();
        res.setToken(token);
        res.setUser(row);
        return res;
    }

    /**
     * 注销当前登录用户：从登录态存储中清除其 Token。
     */
    public void logout() {
        Integer uid = AuthContext.getUserId();
        if (uid != null) {
            tokenStoreService.clear(uid);
        }
    }
}
