package com.example.demo.controller;

import com.example.demo.auth.AuthContext;
import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.util.IdCardUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 当前登录用户个人资料接口：查询本人资料（密码脱敏）与增量更新手机号、
 * 身份证、性别、年龄、住址、密码等信息。
 *
 * @author 丙
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Resource
    private UserMapper userMapper;

    /**
     * 查询当前登录用户资料，返回前清空密码字段。
     *
     * @return 统一包装的用户信息（不含密码），用户不存在时 data 为 null
     */
    @GetMapping
    public Result<User> profile() {
        User u = userMapper.selectById(AuthContext.getUserId());
        if (u != null) {
            u.setPassword(null);
        }
        return Result.success(u);
    }

    /**
     * 增量更新当前登录用户资料：仅覆盖请求体中非空的字段，身份证号经 IdCardUtil 规范化后保存，
     * 密码为空串时不修改。
     *
     * @param body 待更新的用户字段
     * @return 成功返回空成功体；登录用户不存在时返回错误信息
     */
    @PutMapping
    public Result<?> update(@RequestBody User body) {
        User u = userMapper.selectById(AuthContext.getUserId());
        if (u == null) {
            return Result.error("用户不存在");
        }
        if (body.getPhone() != null) {
            u.setPhone(body.getPhone());
        }
        if (body.getIdCard() != null) {
            String ic = IdCardUtil.normalize(body.getIdCard());
            u.setIdCard(ic.isEmpty() ? null : ic);
        }
        if (body.getSex() != null) {
            u.setSex(body.getSex());
        }
        if (body.getAge() != null) {
            u.setAge(body.getAge());
        }
        if (body.getAddress() != null) {
            u.setAddress(body.getAddress());
        }
        if (body.getPassword() != null && !body.getPassword().isEmpty()) {
            u.setPassword(body.getPassword());
        }
        userMapper.updateById(u);
        return Result.success();
    }
}
