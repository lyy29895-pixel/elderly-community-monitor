package com.example.demo;

import com.example.demo.service.HealthDataService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 健康数据服务单元测试
 * 对应模块：健康和环境数据模块（健康画像生成与指标行拆分）
 *
 * 测试 HealthDataService 的静态方法：
 *   - buildMockHealthProfile()：基于老人ID生成稳定的差异化健康画像
 *   - mockSleepStatus()：由画像数组获取睡眠状态中文
 *   - mockOnBedStatus()：由画像数组获取在床状态
 *
 * @author 乙
 */
public class HealthDataServiceTest {

    /** 测试用老人ID */
    private Long elderlyId1;
    private Long elderlyId2;

    /**
     * 测试前初始化：准备两个不同的老人ID，用于验证画像差异化。
     */
    @Before
    public void setUp() {
        elderlyId1 = 1L;    // 对应数据库老人"陈守田"
        elderlyId2 = 2L;    // 对应数据库老人"王秀兰"
    }

    /**
     * 测试1：画像数组不应为null，且长度为8。
     * 索引含义：[0]风险层级 [1]心率 [2]呼吸 [3]睡眠评分 [4]体动 [5]睡眠状态下标 [6]在床 [7]健康评分
     */
    @Test
    public void testProfileNotNull() {
        Object[] profile = HealthDataService.buildMockHealthProfile(elderlyId1);
        assertNotNull("画像数组不应为null", profile);
        assertEquals("画像数组长度应为8", 8, profile.length);
    }

    /**
     * 测试2：心率值应在合理生理范围内（25~150次/分）。
     * 低于30属于极端值，系统设有下限保护。
     */
    @Test
    public void testHeartRateInRange() {
        Object[] profile = HealthDataService.buildMockHealthProfile(elderlyId1);
        int heartRate = (int) profile[1];
        assertTrue("心率应≥28", heartRate >= 28);
        assertTrue("心率应≤130", heartRate <= 130);
    }

    /**
     * 测试3：呼吸频率应在合理生理范围内（6~30次/分）。
     */
    @Test
    public void testBreathingRateInRange() {
        Object[] profile = HealthDataService.buildMockHealthProfile(elderlyId1);
        int breathingRate = (int) profile[2];
        assertTrue("呼吸应≥6", breathingRate >= 6);
        assertTrue("呼吸应≤30", breathingRate <= 30);
    }

    /**
     * 测试4：睡眠评分应在0~100范围内。
     */
    @Test
    public void testSleepScoreInRange() {
        Object[] profile = HealthDataService.buildMockHealthProfile(elderlyId1);
        int sleepScore = (int) profile[3];
        assertTrue("睡眠评分应≥0", sleepScore >= 0);
        assertTrue("睡眠评分应≤100", sleepScore <= 100);
    }

    /**
     * 测试5：健康评分应在0~100范围内，且应落在四个护理等级区间之一。
     * ≥80自理 / 60~79一般护理 / 40~59重点护理 / <40特别护理
     */
    @Test
    public void testHealthScoreInValidRange() {
        Object[] profile = HealthDataService.buildMockHealthProfile(elderlyId1);
        int healthScore = (int) profile[7];
        assertTrue("健康评分应≥0", healthScore >= 0);
        assertTrue("健康评分应≤100", healthScore <= 100);
    }

    /**
     * 测试6：同一老人ID多次调用画像，结果必须一致（稳定性验证）。
     * 画像基于ID哈希生成，同一ID必须产出同一组数据，保证卡片/详情/评估数据口径一致。
     */
    @Test
    public void testProfileStabilityForSameId() {
        Object[] profile1 = HealthDataService.buildMockHealthProfile(elderlyId1);
        Object[] profile2 = HealthDataService.buildMockHealthProfile(elderlyId1);
        assertEquals("同ID心率应一致", profile1[1], profile2[1]);
        assertEquals("同ID呼吸应一致", profile1[2], profile2[2]);
        assertEquals("同ID睡眠评分应一致", profile1[3], profile2[3]);
        assertEquals("同ID健康评分应一致", profile1[7], profile2[7]);
    }

    /**
     * 测试7：不同老人ID的画像应有差异（差异化验证）。
     * 至少有一项指标不同，避免所有老人数据千篇一律。
     */
    @Test
    public void testProfileDifferenceBetweenIds() {
        Object[] profile1 = HealthDataService.buildMockHealthProfile(elderlyId1);
        Object[] profile2 = HealthDataService.buildMockHealthProfile(elderlyId2);
        boolean different = profile1[1] != profile2[1]
                || profile1[2] != profile2[2]
                || profile1[3] != profile2[3];
        assertTrue("不同ID的画像应至少有一项指标不同", different);
    }

    /**
     * 测试8：睡眠状态下标应在0~2范围内（仅3种状态：深睡/浅睡/清醒）。
     */
    @Test
    public void testSleepStatusIndexValid() {
        Object[] profile = HealthDataService.buildMockHealthProfile(elderlyId1);
        int sleepStatusIdx = (int) profile[5];
        assertTrue("睡眠状态下标应≥0", sleepStatusIdx >= 0);
        assertTrue("睡眠状态下标应≤2", sleepStatusIdx <= 2);
    }

    /**
     * 测试9：mockSleepStatus 返回值应为合法的中文状态（深睡/浅睡/清醒）。
     */
    @Test
    public void testSleepStatusString() {
        Object[] profile = HealthDataService.buildMockHealthProfile(elderlyId1);
        String status = HealthDataService.mockSleepStatus(profile);
        assertNotNull("睡眠状态不应为null", status);
        assertTrue("睡眠状态应为深睡/浅睡/清醒之一: " + status,
                status.equals("深睡") || status.equals("浅睡") || status.equals("清醒"));
    }

    /**
     * 测试10：mockOnBedStatus 返回值应为 ON_BED 或 OFF_BED。
     */
    @Test
    public void testOnBedStatusString() {
        Object[] profile = HealthDataService.buildMockHealthProfile(elderlyId1);
        String status = HealthDataService.mockOnBedStatus(profile);
        assertNotNull("在床状态不应为null", status);
        assertTrue("在床状态应为ON_BED或OFF_BED: " + status,
                status.equals("ON_BED") || status.equals("OFF_BED"));
    }

    /**
     * 测试11：null 入参的兜底处理（elderlyId 为 null 时按 ID=1 处理，不应抛异常）。
     */
    @Test
    public void testNullIdDoesNotThrow() {
        Object[] profile = HealthDataService.buildMockHealthProfile(null);
        assertNotNull("null ID入参应返回非null画像（按ID=1兜底）", profile);
        assertEquals("画像长度应为8", 8, profile.length);
    }

    /**
     * 测试12：遍历多个老人ID（1~8），验证每个ID的画像均合法。
     * 对应数据库8位老人，确保全员画像数据均符合生理约束。
     */
    @Test
    public void testAllElderlyProfilesValid() {
        for (long id = 1; id <= 8; id++) {
            Object[] profile = HealthDataService.buildMockHealthProfile(id);
            int heartRate = (int) profile[1];
            int breathingRate = (int) profile[2];
            int sleepScore = (int) profile[3];
            int healthScore = (int) profile[7];
            assertTrue("老人" + id + "心率应≥28", heartRate >= 28);
            assertTrue("老人" + id + "心率应≤130", heartRate <= 130);
            assertTrue("老人" + id + "呼吸应≥6", breathingRate >= 6);
            assertTrue("老人" + id + "呼吸应≤30", breathingRate <= 30);
            assertTrue("老人" + id + "睡眠评分应≥0", sleepScore >= 0);
            assertTrue("老人" + id + "睡眠评分应≤100", sleepScore <= 100);
            assertTrue("老人" + id + "健康评分应≥0", healthScore >= 0);
            assertTrue("老人" + id + "健康评分应≤100", healthScore <= 100);
        }
    }
}
