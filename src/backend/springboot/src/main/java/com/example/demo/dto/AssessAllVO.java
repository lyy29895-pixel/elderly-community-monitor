package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 全量老人一键评估结果
 *
 * @author 丙
 */
@Data
public class AssessAllVO {

    /** 本次评估老人总数 */
    private int total;
    /** 高危人数 */
    private int highCount;
    /** 中危人数 */
    private int mediumCount;
    /** 低危人数 */
    private int lowCount;
    /** 评估时间 */
    private LocalDateTime evaluatedAt;

    /** 逐人评估明细列表（按风险等级与未处理事件数排序） */
    private List<AssessItem> items = new ArrayList<>();

    /**
     * 单个老人的评估结果项：关键监测指标、未处理事件数与评估结论，
     * 结论已同步保存到老人档案。
     */
    @Data
    public static class AssessItem {
        /** 老人ID */
        private Long elderlyId;
        /** 老人姓名 */
        private String name;
        /** 房间号 */
        private String room;

        // 本次评估使用的关键监测指标
        /** 心率（次/分） */
        private Integer heartRate;
        /** 呼吸频率（次/分） */
        private Integer breathingRate;
        /** 睡眠评分 */
        private Double sleepScore;
        /** 睡眠状态（深睡/浅睡/清醒等） */
        private String sleepStatus;
        /** 未处理事件数 */
        private int openAlertCount;

        // 评估结论（已同步保存到老人档案）
        /** HIGH / MEDIUM / LOW */
        private String riskLevel;
        /** 0-100 */
        private Integer healthScore;
        /** LEVEL_1 ~ LEVEL_4，由健康分按硬规则确定 */
        private String nursingLevel;
    }
}
