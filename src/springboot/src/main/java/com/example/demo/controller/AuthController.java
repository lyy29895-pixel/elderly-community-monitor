package com.example.demo.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.auth.AuthService;
import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.enums.UserRole;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.UserMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 认证相关接口：提供登录、社区账号开放注册与注销，接口前缀 /api/auth。
 *
 * @author 甲
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;
    @Resource
    private UserMapper userMapper;

    /**
     * 用户登录接口，对用户名做去首尾空格处理后交由认证服务校验。
     *
     * @param user 请求体中的用户名与密码
     * @return 包含 JWT 与用户信息的统一响应
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user) {
        String u = user.getUsername() == null ? null : user.getUsername().trim();
        return Result.success(authService.login(u, user.getPassword()));
    }

    /**
     * 社区工作人员账号开放注册接口：校验用户名唯一性，密码缺省为 123456，
     * 仅允许注册 community 角色，归一化后写入用户表。
     *
     * @param user 请求体中的注册信息
     * @return 不携带数据的成功统一响应
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        user.setUsername(user.getUsername().trim());
        User exists = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, user.getUsername()));
        if (exists != null) {
            throw new BusinessException("用户名已存在");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456");
        }
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole(UserRole.COMMUNITY);
        }
        user.setRole(UserRole.normalize(user.getRole()));
        if (!UserRole.COMMUNITY.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException("开放注册仅支持社区工作人员账号（community）");
        }
        userMapper.insert(user);
        return Result.success();
    }

    /**
     * 注销当前登录用户，清除服务端登录态。
     *
     * @return 不携带数据的成功统一响应
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        authService.logout();
        return Result.success();
    }
}
