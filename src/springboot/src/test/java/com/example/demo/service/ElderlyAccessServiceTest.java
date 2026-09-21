package com.example.demo.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.demo.auth.AuthContext;
import com.example.demo.entity.Elderly;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.ElderlyMapper;
import com.example.demo.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 老人数据访问权限服务单元测试：覆盖各角色的档案访问判定、
 * 可访问 ID 集合与社区工作人员权限断言（Mockito 隔离数据库）。
 *
 * @author 甲
 */
public class ElderlyAccessServiceTest {

    private static final String NO_ACCESS_MSG = "无权限访问该老人数据";

    private ElderlyMapper elderlyMapper;
    private UserMapper userMapper;
    private ElderlyAccessService accessService;

    @BeforeClass
    public static void initTableInfo() {
        // 纯单测环境无 MyBatis 启动流程，需手动注册实体的 Lambda 列缓存
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Elderly.class);
    }

    @Before
    public void setUp() {
        AuthContext.clear();
        elderlyMapper = Mockito.mock(ElderlyMapper.class);
        userMapper = Mockito.mock(UserMapper.class);
        accessService = new ElderlyAccessService();
        ReflectionTestUtils.setField(accessService, "elderlyMapper", elderlyMapper);
        ReflectionTestUtils.setField(accessService, "userMapper", userMapper);
    }

    @After
    public void tearDown() {
        AuthContext.clear();
    }

    /** 构造一条老人档案。 */
    private Elderly elderly(long id, String elderUsername) {
        Elderly e = new Elderly();
        e.setId(id);
        e.setElderUsername(elderUsername);
        return e;
    }

    /** 构造一个登录用户行。 */
    private User user(int id, String username) {
        User u = new User();
        u.setUserId(id);
        u.setUsername(username);
        return u;
    }

    // ---------- assertCanAccessElderly ----------

    /** 档案 ID 为 null 或档案不存在时抛 404。 */
    @Test
    public void assertRejectsNullOrMissingElderly() {
        try {
            accessService.assertCanAccessElderly(null);
            fail("null ID 应抛 404");
        } catch (BusinessException e) {
            assertEquals("404", e.getCode());
        }

        when(elderlyMapper.selectById(99L)).thenReturn(null);
        AuthContext.set(1, "admin");
        try {
            accessService.assertCanAccessElderly(99L);
            fail("档案不存在应抛 404");
        } catch (BusinessException e) {
            assertEquals("404", e.getCode());
        }
    }

    /** 管理员与社区工作人员可访问任意档案。 */
    @Test
    public void assertAdminAndCommunityPass() {
        when(elderlyMapper.selectById(1L)).thenReturn(elderly(1, "张三"));

        AuthContext.set(1, "admin");
        accessService.assertCanAccessElderly(1L);

        AuthContext.set(2, "community");
        accessService.assertCanAccessElderly(1L);
    }

    /** 老人可访问与自己用户名关联的档案。 */
    @Test
    public void assertElderOwnRecordPasses() {
        when(elderlyMapper.selectById(1L)).thenReturn(elderly(1, "王爷爷"));
        when(userMapper.selectById(3)).thenReturn(user(3, "王爷爷"));
        AuthContext.set(3, "elder");

        accessService.assertCanAccessElderly(1L);
    }

    /** 老人访问他人档案被拒绝（403）。 */
    @Test
    public void assertElderOtherRecordRejected() {
        when(elderlyMapper.selectById(1L)).thenReturn(elderly(1, "王爷爷"));
        when(userMapper.selectById(3)).thenReturn(user(3, "李爷爷"));
        AuthContext.set(3, "elder");

        try {
            accessService.assertCanAccessElderly(1L);
            fail("老人访问他人档案应被拒绝");
        } catch (BusinessException e) {
            assertEquals("403", e.getCode());
            assertEquals(NO_ACCESS_MSG, e.getMessage());
        }
    }

    /** 子女角色访问档案被拒绝（403）。 */
    @Test
    public void assertChildRejected() {
        when(elderlyMapper.selectById(1L)).thenReturn(elderly(1, "王爷爷"));
        AuthContext.set(4, "child");

        try {
            accessService.assertCanAccessElderly(1L);
            fail("子女角色不应能访问档案详情");
        } catch (BusinessException e) {
            assertEquals("403", e.getCode());
        }
    }

    // ---------- canUserAccessElderly ----------

    /** 参数缺失或档案不存在时返回 false。 */
    @Test
    public void canAccessReturnsFalseForBadInput() {
        assertFalse(accessService.canUserAccessElderly(null, "admin", 1L));
        assertFalse(accessService.canUserAccessElderly(1, "admin", null));
        when(elderlyMapper.selectById(99L)).thenReturn(null);
        assertFalse(accessService.canUserAccessElderly(1, "admin", 99L));
    }

    /** 布尔判定：管理员/社区通过，老人仅本人，子女拒绝。 */
    @Test
    public void canAccessByRole() {
        when(elderlyMapper.selectById(1L)).thenReturn(elderly(1, "王爷爷"));
        when(userMapper.selectById(3)).thenReturn(user(3, "王爷爷"));

        assertTrue(accessService.canUserAccessElderly(1, "admin", 1L));
        assertTrue(accessService.canUserAccessElderly(2, "community", 1L));
        assertTrue(accessService.canUserAccessElderly(3, "elder", 1L));
        when(userMapper.selectById(3)).thenReturn(user(3, "李爷爷"));
        assertFalse(accessService.canUserAccessElderly(3, "elder", 1L));
        assertFalse(accessService.canUserAccessElderly(4, "child", 1L));
        assertFalse(accessService.canUserAccessElderly(5, "unknown", 1L));
    }

    // ---------- getAccessibleElderlyIds ----------

    /** 社区/管理员返回全部档案 ID。 */
    @Test
    public void accessibleIdsForCommunity() {
        when(elderlyMapper.selectList(any()))
                .thenReturn(Arrays.asList(elderly(1, "张三"), elderly(2, "李四")));

        List<Long> ids = accessService.getAccessibleElderlyIds(1, "community");
        assertEquals(Arrays.asList(1L, 2L), ids);
    }

    /** 老人返回本人档案 ID；无档案时为空。 */
    @Test
    public void accessibleIdsForElder() {
        when(userMapper.selectById(3)).thenReturn(user(3, "王爷爷"));
        when(elderlyMapper.selectOne(any())).thenReturn(elderly(7, "王爷爷"));

        assertEquals(Collections.singletonList(7L), accessService.getAccessibleElderlyIds(3, "elder"));

        when(userMapper.selectById(3)).thenReturn(user(3, "无档案老人"));
        when(elderlyMapper.selectOne(any())).thenReturn(null);
        assertTrue(accessService.getAccessibleElderlyIds(3, "elder").isEmpty());
    }

    /** 子女与其他角色返回空列表。 */
    @Test
    public void accessibleIdsForChildAndUnknown() {
        assertTrue(accessService.getAccessibleElderlyIds(4, "child").isEmpty());
        assertTrue(accessService.getAccessibleElderlyIds(5, "unknown").isEmpty());
    }

    // ---------- requireCommunityStaff ----------

    /** 管理员与社区工作人员通过权限断言。 */
    @Test
    public void requireStaffPassesForAdminAndCommunity() {
        AuthContext.set(1, "admin");
        accessService.requireCommunityStaff();

        AuthContext.set(2, "COMMUNITY");
        accessService.requireCommunityStaff();
    }

    /** 老人角色不满足社区工作人员权限，抛 403。 */
    @Test
    public void requireStaffRejectsElder() {
        AuthContext.set(3, "elder");
        try {
            accessService.requireCommunityStaff();
            fail("老人角色应被拒绝");
        } catch (BusinessException e) {
            assertEquals("403", e.getCode());
            assertEquals("需要社区工作人员权限", e.getMessage());
        }
    }
}
