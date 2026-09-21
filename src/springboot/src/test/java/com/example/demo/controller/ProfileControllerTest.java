package com.example.demo.controller;

import com.example.demo.auth.AuthContext;
import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 个人中心接口单元测试：覆盖本人资料查询（密码脱敏）与
 * 增量更新（手机号/身份证规范化/密码空串忽略）等分支（Mockito 隔离数据库）。
 * */
public class ProfileControllerTest {

    private UserMapper userMapper;
    private ProfileController profileController;

    @Before
    public void setUp() {
        AuthContext.clear();
        userMapper = Mockito.mock(UserMapper.class);
        profileController = new ProfileController();
        ReflectionTestUtils.setField(profileController, "userMapper", userMapper);
    }

    @After
    public void tearDown() {
        AuthContext.clear();
    }

    /** 构造一个已登录的数据库用户。 */
    private User dbUser() {
        User u = new User();
        u.setUserId(3);
        u.setUsername("admin1");
        u.setPassword("secret");
        return u;
    }

    /** 查询本人资料：返回用户信息且密码脱敏。 */
    @Test
    public void profileMasksPassword() {
        AuthContext.set(3, "community");
        when(userMapper.selectById(3)).thenReturn(dbUser());

        Result<User> res = profileController.profile();

        assertEquals("200", res.getCode());
        assertEquals("admin1", res.getData().getUsername());
        assertNull("查询结果必须清空密码", res.getData().getPassword());
    }

    /** 查询资料：当前登录用户不存在时 data 为 null 但响应仍为成功。 */
    @Test
    public void profileMissingUserReturnsNullData() {
        AuthContext.set(99, "community");
        when(userMapper.selectById(99)).thenReturn(null);

        Result<User> res = profileController.profile();

        assertEquals("200", res.getCode());
        assertNull(res.getData());
    }

    /** 更新资料：仅覆盖非空字段，身份证经规范化保存，空串密码不修改。 */
    @Test
    public void updateAppliesOnlyProvidedFields() {
        AuthContext.set(3, "community");
        User existing = dbUser();
        existing.setPassword("old-pass");
        when(userMapper.selectById(3)).thenReturn(existing);

        User body = new User();
        body.setPhone("13800000000");
        body.setIdCard(" 110101 19900307 891x ");
        body.setSex("男");
        body.setAge(35);
        body.setAddress("幸福路1号");
        body.setPassword("");

        Result<?> res = profileController.update(body);

        assertEquals("200", res.getCode());
        assertEquals("13800000000", existing.getPhone());
        assertEquals("11010119900307891X", existing.getIdCard());
        assertEquals("男", existing.getSex());
        assertEquals(Integer.valueOf(35), existing.getAge());
        assertEquals("幸福路1号", existing.getAddress());
        assertEquals("空串密码不应覆盖原密码", "old-pass", existing.getPassword());
        verify(userMapper).updateById(existing);
    }

    /** 更新资料：身份证仅含空白时置为 null。 */
    @Test
    public void updateBlankIdCardSetsNull() {
        AuthContext.set(3, "community");
        User existing = dbUser();
        when(userMapper.selectById(3)).thenReturn(existing);

        User body = new User();
        body.setIdCard("   ");

        profileController.update(body);

        assertNull(existing.getIdCard());
    }

    /** 更新资料：传入新密码时正常覆盖。 */
    @Test
    public void updateNewPasswordOverwrites() {
        AuthContext.set(3, "community");
        User existing = dbUser();
        when(userMapper.selectById(3)).thenReturn(existing);

        User body = new User();
        body.setPassword("new-pass");

        profileController.update(body);

        assertEquals("new-pass", existing.getPassword());
    }

    /** 更新资料：登录用户不存在时返回错误响应。 */
    @Test
    public void updateMissingUserReturnsError() {
        AuthContext.set(99, "community");
        when(userMapper.selectById(99)).thenReturn(null);

        Result<?> res = profileController.update(new User());

        assertEquals("500", res.getCode());
        assertEquals("用户不存在", res.getMsg());
    }
}
