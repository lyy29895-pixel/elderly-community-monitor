package com.example.demo.dto;

import com.example.demo.entity.User;
import lombok.Data;

/**
 * 登录成功响应 DTO：携带签发的 JWT 与脱敏后的用户信息。
 * */
@Data
public class LoginResponse {
    /** 登录成功后签发的 JWT 令牌 */
    private String token;
    /** 当前登录用户信息（密码字段不返回） */
    private User user;
}
