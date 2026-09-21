package com.example.demo;

import com.example.demo.auth.AuthContext;
import com.example.demo.dto.AssessAllVO;
import com.example.demo.dto.CommunityOverviewVO;
import com.example.demo.service.CommunityOverviewService;
import com.example.demo.service.ElderlyAccessService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * 社区概览与一键评估模块集成测试
 * 对应模块：社区概览模块、一键评估模块
 *
 * 使用 @SpringBootTest + @RunWith(SpringRunner.class) 启动完整 Spring 上下文，
 * 连接腾讯云 MySQL 数据库，验证 Service 层与数据库的真实交互。
 *
 * 测试前需确保：
 *   1. 后端服务已停止（避免端口9090冲突，本测试不启动Web服务器）
 *   2. 数据库连接正常（腾讯云 CynosDB community 库）
 *   3. elder_profiles 表有8条初始数据
 * */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CommunityOverviewServiceTest {

    @Autowired
    private CommunityOverviewService communityOverviewService;

    @Autowired
    private ElderlyAccessService elderlyAccessService;

    /** 缓存概览结果，避免重复调用 overview() */
    private CommunityOverviewVO cachedOverview;

    /**
     * 测试前设置登录上下文：模拟社区工作人员登录。
     * AuthContext 基于 ThreadLocal，测试线程中无 HTTP 请求拦截器填充，
     * 需手动设置用户ID和角色，否则 overview() / assessAll() 会抛403权限异常。
     */
    @Before
    public void setUp() {
        // 模拟 community 角色登录（users 表 id=2, role=community）
        AuthContext.set(2, "community");
    }

    /**
     * 测试后清理 ThreadLocal，防止线程复用导致上下文泄漏。
     */
    @After
    public void tearDown() {
        AuthContext.clear();
    }

    /**
     * 辅助方法：获取缓存的概览数据（多个测试共用同一份结果）。
     */
    private CommunityOverviewVO getOverview() {
        if (cachedOverview == null) {
            cachedOverview = communityOverviewService.overview();
        }
        return cachedOverview;
    }

    /**
     * 测试1：overview() 方法返回非null的概览数据对象。
     * 验证 Spring 上下文正常启动，Service 依赖注入成功，数据库连接正常。
     */
    @Test
    public void testOverviewReturnsResult() {
        CommunityOverviewVO vo = getOverview();
        assertNotNull("概览数据不应为null", vo);
    }

    /**
     * 测试2：老人总数应大于0（数据库 elder_profiles 表有实际数据）。
     */
    @Test
    public void testElderlyTotal() {
        CommunityOverviewVO vo = getOverview();
        assertTrue("老人总数应大于0", vo.getElderlyTotal() > 0);
    }

    /**
     * 测试3：风险列表不应为空，且每位老人风险行应有姓名。
     */
    @Test
    public void testRiskListNotEmpty() {
        CommunityOverviewVO vo = getOverview();
        assertNotNull("风险列表不应为null", vo.getRiskList());
        assertFalse("风险列表不应为空", vo.getRiskList().isEmpty());
        // 验证第一项风险行有姓名
        CommunityOverviewVO.ElderlyRiskRow firstRow = vo.getRiskList().get(0);
        assertNotNull("第一行老人姓名不应为null", firstRow.getName());
        assertFalse("第一行老人姓名不应为空串", firstRow.getName().isEmpty());
    }

    /**
     * 测试4：风险列表应按风险等级由高到低排序。
     * HIGH 排在 MEDIUM 之前，MEDIUM 排在 LOW 之前。
     */
    @Test
    public void testRiskListSortedByRiskLevel() {
        CommunityOverviewVO vo = getOverview();
        java.util.List<CommunityOverviewVO.ElderlyRiskRow> list = vo.getRiskList();
        if (list.size() < 2) return; // 不足2行无法验证排序

        for (int i = 0; i < list.size() - 1; i++) {
            String current = list.get(i).getRiskLevel();
            String next = list.get(i + 1).getRiskLevel();
            int currentRank = riskRank(current);
            int nextRank = riskRank(next);
            assertTrue("风险列表应按等级降序排列（第" + i + "行" + current
                    + "不应排在第" + (i + 1) + "行" + next + "之后）",
                    currentRank <= nextRank);
        }
    }

    /**
     * 辅助方法：将风险等级字符串转为排序键（HIGH=0, MEDIUM=1, LOW=2, 其他=3）。
     */
    private int riskRank(String level) {
        if ("HIGH".equals(level)) return 0;
        if ("MEDIUM".equals(level)) return 1;
        if ("LOW".equals(level)) return 2;
        return 3;
    }

    /**
     * 测试5：性别统计应合理（男性+女性人数 ≤ 老人总数，部分老人性别可能为null）。
     */
    @Test
    public void testGenderStats() {
        CommunityOverviewVO vo = getOverview();
        int gendered = vo.getMaleCount() + vo.getFemaleCount();
        assertTrue("男性+女性人数应≤老人总数", gendered <= vo.getElderlyTotal());
    }

    /**
     * 测试6：一键评估 assessAll() 应返回非null结果，且评估总人数大于0。
     */
    @Test
    public void testAssessAllReturnsResult() {
        AssessAllVO vo = communityOverviewService.assessAll();
        assertNotNull("评估结果不应为null", vo);
        assertTrue("评估总人数应大于0", vo.getTotal() > 0);
    }

    /**
     * 测试7：一键评估后，高/中/低风险人数之和应等于总评估人数。
     */
    @Test
    public void testAssessAllRiskDistribution() {
        AssessAllVO vo = communityOverviewService.assessAll();
        int sum = vo.getHighCount() + vo.getMediumCount() + vo.getLowCount();
        assertEquals("高+中+低风险人数应等于总人数", vo.getTotal(), sum);
    }

    /**
     * 测试8：一键评估后，每个评估明细项应有姓名和风险等级。
     */
    @Test
    public void testAssessItemsHaveNameAndRisk() {
        AssessAllVO vo = communityOverviewService.assessAll();
        assertNotNull("评估明细列表不应为null", vo.getItems());
        assertFalse("评估明细列表不应为空", vo.getItems().isEmpty());
        for (AssessAllVO.AssessItem item : vo.getItems()) {
            assertNotNull("评估项姓名不应为null", item.getName());
            assertFalse("评估项姓名不应为空串", item.getName().isEmpty());
            assertNotNull("评估项风险等级不应为null", item.getRiskLevel());
        }
    }

    /**
     * 测试9：一键评估后，健康评分应在0~100范围内。
     */
    @Test
    public void testAssessHealthScoreInRange() {
        AssessAllVO vo = communityOverviewService.assessAll();
        for (AssessAllVO.AssessItem item : vo.getItems()) {
            int score = item.getHealthScore();
            assertTrue("老人" + item.getName() + "健康评分应≥0", score >= 0);
            assertTrue("老人" + item.getName() + "健康评分应≤100", score <= 100);
        }
    }

    /**
     * 测试10：一键评估后，护理等级应为 LEVEL_1 ~ LEVEL_4 之一。
     */
    @Test
    public void testAssessNursingLevelValid() {
        AssessAllVO vo = communityOverviewService.assessAll();
        for (AssessAllVO.AssessItem item : vo.getItems()) {
            String level = item.getNursingLevel();
            assertNotNull("护理等级不应为null", level);
            assertTrue("护理等级" + level + "应为LEVEL_1~4之一",
                    level.equals("LEVEL_1") || level.equals("LEVEL_2")
                            || level.equals("LEVEL_3") || level.equals("LEVEL_4"));
        }
    }

    /**
     * 测试11：权限校验——社区工作人员可访问存在的老人档案。
     * 从概览结果中取第一位老人ID进行验证，避免硬编码ID。
     */
    @Test
    public void testCommunityStaffCanAccessElderly() {
        CommunityOverviewVO vo = getOverview();
        assertFalse("风险列表不应为空", vo.getRiskList().isEmpty());
        Long elderlyId = vo.getRiskList().get(0).getElderlyId();
        assertNotNull("第一位老人ID不应为null", elderlyId);
        // 不抛异常即通过
        elderlyAccessService.assertCanAccessElderly(elderlyId);
    }

    /**
     * 测试12：权限校验——不存在的老人ID应抛出 BusinessException。
     */
    @Test(expected = com.example.demo.exception.BusinessException.class)
    public void testAccessNonExistentElderlyThrows() {
        elderlyAccessService.assertCanAccessElderly(99999L);
    }
}
