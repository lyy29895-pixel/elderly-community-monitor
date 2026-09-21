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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 老人档案服务单元测试：覆盖按角色的列表可见性、详情查询权限委托、
 * 新建（姓名规范化与 elderUserId 顺延）、修改与删除（Mockito 隔离数据库）。
 * */
public class ElderlyServiceTest {

    private ElderlyMapper elderlyMapper;
    private UserMapper userMapper;
    private ElderlyAccessService elderlyAccessService;
    private ElderlyService elderlyService;

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
        elderlyAccessService = Mockito.mock(ElderlyAccessService.class);
        elderlyService = new ElderlyService();
        ReflectionTestUtils.setField(elderlyService, "elderlyMapper", elderlyMapper);
        ReflectionTestUtils.setField(elderlyService, "userMapper", userMapper);
        ReflectionTestUtils.setField(elderlyService, "elderlyAccessService", elderlyAccessService);
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
        e.setRealName(elderUsername);
        return e;
    }

    // ---------- listForCurrentUser ----------

    /** 社区角色可见全部档案。 */
    @Test
    public void listCommunitySeesAll() {
        AuthContext.set(1, "community");
        List<Elderly> all = Arrays.asList(elderly(1, "张三"), elderly(2, "李四"));
        when(elderlyMapper.selectList(any())).thenReturn(all);

        assertSame(all, elderlyService.listForCurrentUser());
    }

    /** 管理员角色可见全部档案。 */
    @Test
    public void listAdminSeesAll() {
        AuthContext.set(1, "admin");
        List<Elderly> all = Collections.singletonList(elderly(1, "张三"));
        when(elderlyMapper.selectList(any())).thenReturn(all);

        assertSame(all, elderlyService.listForCurrentUser());
    }

    /** 老人角色仅可见与自己用户名关联的一份档案。 */
    @Test
    public void listElderSeesOwnOnly() {
        AuthContext.set(2, "elder");
        User u = new User();
        u.setUserId(2);
        u.setUsername("王爷爷");
        when(userMapper.selectById(2)).thenReturn(u);
        Elderly own = elderly(9, "王爷爷");
        when(elderlyMapper.selectOne(any())).thenReturn(own);

        List<Elderly> result = elderlyService.listForCurrentUser();
        assertEquals(1, result.size());
        assertEquals("王爷爷", result.get(0).getElderUsername());
    }

    /** 老人账号不存在时返回空列表。 */
    @Test
    public void listElderWithoutUserReturnsEmpty() {
        AuthContext.set(2, "elder");
        when(userMapper.selectById(2)).thenReturn(null);

        assertTrue("应返回空列表", elderlyService.listForCurrentUser().isEmpty());
    }

    /** 子女角色不开放档案查看，返回空列表。 */
    @Test
    public void listChildReturnsEmpty() {
        AuthContext.set(3, "child");
        assertTrue(elderlyService.listForCurrentUser().isEmpty());
        verify(elderlyMapper, never()).selectList(any());
    }

    // ---------- getByIdForCurrentUser ----------

    /** 详情查询先做权限校验再返回档案。 */
    @Test
    public void getByIdChecksAccessFirst() {
        Elderly e = elderly(9, "张三");
        when(elderlyMapper.selectById(9L)).thenReturn(e);

        Elderly result = elderlyService.getByIdForCurrentUser(9L);

        verify(elderlyAccessService).assertCanAccessElderly(9L);
        assertSame(e, result);
    }

    // ---------- create ----------

    /** 新建档案：姓名去空格、回填 realName/elderUsername、elderUserId 按最大值顺延。 */
    @Test
    public void createNormalizesNameAndGeneratesElderUserId() {
        AuthContext.set(1, "community");
        Elderly last = elderly(3, "旧老人");
        last.setElderUserId(7L);
        when(elderlyMapper.selectOne(any())).thenReturn(last);

        Elderly input = new Elderly();
        input.setName("  王五  ");
        Elderly saved = elderlyService.create(input);

        assertEquals("王五", saved.getRealName());
        assertEquals("王五", saved.getElderUsername());
        assertEquals(Long.valueOf(8L), saved.getElderUserId());
        verify(elderlyMapper).insert(saved);
    }

    /** 新建档案：未传 name 时回退使用 realName。 */
    @Test
    public void createFallsBackToRealName() {
        AuthContext.set(1, "community");
        when(elderlyMapper.selectOne(any())).thenReturn(null);

        Elderly input = new Elderly();
        input.setRealName("赵六");
        Elderly saved = elderlyService.create(input);

        assertEquals("赵六", saved.getElderUsername());
        assertEquals("无既有档案时 elderUserId 从 1 开始", Long.valueOf(1L), saved.getElderUserId());
    }

    /** 新建档案：姓名为空时拒绝。 */
    @Test
    public void createRejectsBlankName() {
        AuthContext.set(1, "community");
        Elderly input = new Elderly();

        try {
            elderlyService.create(input);
            fail("姓名为空应抛业务异常");
        } catch (BusinessException e) {
            assertEquals("姓名不能为空", e.getMessage());
        }
        verify(elderlyMapper, never()).insert(any(Elderly.class));
    }

    /** 新建档案：非社区管理类角色被权限服务拒绝。 */
    @Test
    public void createRequiresCommunityStaff() {
        AuthContext.set(4, "elder");
        doThrow(new BusinessException("403", "需要社区工作人员权限"))
                .when(elderlyAccessService).requireCommunityStaff();

        try {
            elderlyService.create(elderly(0, "张三"));
            fail("老人角色不应能新建档案");
        } catch (BusinessException e) {
            assertEquals("403", e.getCode());
        }
        verify(elderlyMapper, never()).insert(any(Elderly.class));
    }

    // ---------- update ----------

    /** 修改档案：传入 name 时同步更新 realName。 */
    @Test
    public void updateSyncsRealName() {
        AuthContext.set(1, "community");
        Elderly input = elderly(3, null);
        input.setName("新名字");

        elderlyService.update(input);

        verify(elderlyAccessService).assertCanAccessElderly(3L);
        assertEquals("新名字", input.getRealName());
        verify(elderlyMapper).updateById(input);
    }

    /** 修改档案：name 为空时仅按非空字段更新。 */
    @Test
    public void updateWithoutNameKeepsRealName() {
        AuthContext.set(1, "community");
        Elderly input = elderly(3, "旧名");
        input.setAge(88);

        elderlyService.update(input);

        assertEquals("旧名", input.getRealName());
        verify(elderlyMapper).updateById(input);
    }

    // ---------- delete ----------

    /** 删除档案：存在时按 ID 删除。 */
    @Test
    public void deleteExisting() {
        AuthContext.set(1, "community");
        when(elderlyMapper.selectById(5L)).thenReturn(elderly(5, "张三"));

        elderlyService.delete(5L);

        verify(elderlyAccessService).assertCanAccessElderly(5L);
        verify(elderlyMapper).deleteById(5L);
    }

    /** 删除档案：不存在时抛 404 且不执行删除。 */
    @Test
    public void deleteMissingThrows404() {
        AuthContext.set(1, "community");
        when(elderlyMapper.selectById(99L)).thenReturn(null);

        try {
            elderlyService.delete(99L);
            fail("删除不存在的档案应抛 404");
        } catch (BusinessException e) {
            assertEquals("404", e.getCode());
            assertEquals("老人档案不存在", e.getMessage());
        }
        verify(elderlyMapper, never()).deleteById(any());
    }
}
