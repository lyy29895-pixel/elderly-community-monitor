package com.example.demo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 辖区概览视图对象：顶部汇总指标、整体统计数据（性别/年龄/设备绑定/事件解决情况）
 * 与逐人风险行列表。
 *
 * @author 丙
 */
@Data
public class CommunityOverviewVO {
    /** 社区ID（当前不按社区过滤，固定为 null） */
    private Long communityId;
    /** 老人总数 */
    private int elderlyTotal;
    /** 待处理事件数（仅 NEW/未处理 口径） */
    private int pendingAlertTotal;
    /** 紧急未关闭事件数（严重等级≥3 且尚未关闭） */
    private int criticalPendingTotal;
    /** 近 7 天新增事件数 */
    private int newAlertsLast7Days;

    // ===== 整体统计数据（新增） =====
    /** 男性人数 */
    private int maleCount;                     // 男性人数
    /** 女性人数 */
    private int femaleCount;                   // 女性人数
    /** 70岁以下人数 */
    private int ageUnder70;                    // 70岁以下
    /** 70~80岁人数 */
    private int age70to80;                     // 70~80岁
    /** 80岁以上人数 */
    private int ageOver80;                     // 80岁以上
    /** 已绑定监护设备人数 */
    private int deviceBoundCount;              // 已绑定设备人数
    /** 未绑定监护设备人数 */
    private int deviceUnboundCount;            // 未绑定设备人数
    /** 事件总数 */
    private int alertEventTotal;               // 事件总数
    /** 已解决事件数 */
    private int alertResolvedCount;            // 已解决事件数
    /** 未解决事件数 */
    private int alertUnresolvedCount;          // 未解决事件数

    /** 逐人风险列表，按风险等级、未关闭事件数、老人ID排序 */
    private List<ElderlyRiskRow> riskList = new ArrayList<>();

    /**
     * 单个老人的风险评估行：基础信息、未关闭事件、生命体征/睡眠画像、
     * 居室环境数据与本次评估结论。
     */
    @Data
    public static class ElderlyRiskRow {
        /** 老人ID */
        private Long elderlyId;
        /** 老人姓名 */
        private String name;
        /** 性别 */
        private String gender;
        /** 年龄 */
        private Integer age;
        /** 房间号 */
        private String room;
        /** 联系电话 */
        private String phone;
        /** 家庭住址 */
        private String address;
        /** 风险等级：HIGH 高危 / MEDIUM 中危 / LOW 低危 */
        private String riskLevel;
        /** 未关闭事件数 */
        private int openAlertCount;
        /** 未关闭事件的最高严重等级（无未关闭事件时为 null） */
        private Integer maxOpenSeverity;
        /** 最近一次健康评分 */
        private Integer lastHealthScore;
        /** 最近数据上报日期 */
        private String lastReportDate;
        /** 风险提示语列表 */
        private List<String> hints = new ArrayList<>();

        /** 心率（次/分） */
        private Integer heartRate;
        /** 呼吸频率（次/分） */
        private Integer breathingRate;
        /** 睡眠状态（深睡/浅睡/清醒等） */
        private String sleepStatus;
        /** 睡眠评分 */
        private Double sleepScore;
        /** 本次计算的健康评分（0~100） */
        private Integer healthScore;
        /** 在床状态 */
        private String onBedStatus;
        /** 姿态状态 */
        private String postureStatus;
        /** 绑定的监护设备ID */
        private String monitorDeviceId;
        /** 最近健康数据采集时间 */
        private String lastHealthTime;

        // 环境数据
        /** 居室环境温度（℃） */
        private Double temperature;
        /** 居室环境湿度（%） */
        private Double humidity;
        /** 环境数据采集时间 */
        private String envTime;
    }
}
