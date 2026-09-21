package com.example.demo.auth;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.demo.dto.LoginResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.util.JwtUtil;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务单元测试：覆盖登录成功、各类登录失败与注销逻辑（Mockito 隔离数据库）。
 *
 * @author 甲
 */
public class AuthServiceTest {

    private UserMapper userMapper;
    private JwtUtil jwtUtil;
    private TokenStoreService tokenStoreService;
    private AuthService authService;

    @BeforeClass
    public static void initTableInfo() {
        // 纯单测环境无 MyBatis 启动流程，需手动注册实体的 Lambda 列缓存
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
    }

    @Before
    public void setUp() {
        AuthContext.clear();
        userMapper = Mockito.mock(UserMapper.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        tokenStoreService = Mockito.mock(TokenStoreService.class);
        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "userMapper", userMapper);
        ReflectionTestUtils.setField(authService, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(authService, "tokenStoreService", tokenStoreService);
    }

    /** 构造一个可登录的数据库用户行。 */
    private User dbUser() {
        User u = new User();
        u.setUserId(5);
        u.setUsername("admin1");
        u.setPassword("123456");
        u.setRole("COMMUNITY");
        return u;
    }

    /** 登录成功：签发令牌、保存登录态、角色归一化且密码脱敏。 */
    @Test
    public void loginSuccess() {
        when(userMapper.selectOne(any())).thenReturn(dbUser());
        when(jwtUtil.createToken(5, "admin1", "community")).thenReturn("jwt-token");

        LoginResponse res = authService.login("admin1", "123456");

        assertEquals("jwt-token", res.getToken());
        assertEquals("community", res.getUser().getRole());
        assertNull("返回用户信息必须脱敏密码", res.getUser().getPassword());
        verify(tokenStoreService).saveToken(5, "jwt-token");
    }

    /** 用户名为 null 或空串时直接拒绝，不查库。 */
    @Test
    public void loginRejectsBlankUsername() {
        assertLoginError(null, "123456");
        assertLoginError("", "123456");
        verify(userMapper, never()).selectOne(any());
    }

    /** 账号不存在或密码错误时统一提示“用户名或密码错误”。 */
    @Test
    public void loginRejectsWrongPasswordOrMissingUser() {
        when(userMapper.selectOne(any())).thenReturn(null);
        assertLoginError("ghost", "123456");

        when(userMapper.selectOne(any())).thenReturn(dbUser());
        assertLoginError("admin1", "bad-password");
    }

    /** 已下线的 FAMILY 角色禁止登录。 */
    @Test
    public void loginRejectsFamilyRole() {
        User u = dbUser();
        u.setRole("FAMILY");
        when(userMapper.selectOne(any())).thenReturn(u);

        try {
            authService.login("admin1", "123456");
            fail("应拒绝 FAMILY 角色登录");
        } catch (BusinessException e) {
            assertEquals("403", e.getCode());
        }
        verify(tokenStoreService, never()).saveToken(anyInt(), anyString());
    }

    /** 注销：当前线程持有用户 ID 时清除其登录态。 */
    @Test
    public void logoutClearsTokenStore() {
        AuthContext.set(7, "community");
        authService.logout();
        verify(tokenStoreService).clear(7);
    }

    /** 注销：未登录（上下文无用户 ID）时不触发清除。 */
    @Test
    public void logoutWithoutLoginDoesNothing() {
        authService.logout();
        verify(tokenStoreService, never()).clear(anyInt());
    }

    /** 断言登录抛出 401 业务异常。 */
    private void assertLoginError(String username, String password) {
        try {
            authService.login(username, password);
            fail("应抛出登录失败业务异常");
        } catch (BusinessException e) {
            assertEquals("401", e.getCode());
            assertEquals("用户名或密码错误", e.getMessage());
        }
    }
}
