package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 健康数据实体类
 * 对应数据库表 health_history，承载心率、呼吸、睡眠、体动、在床状态等健康监测指标；
 * 另含若干非表字段，用于业务层传递老人ID以及前端指标行（metricType/metricValue/unit）展示。
 *
 * @author 丙
 */
@Data
@TableName("health_history")
public class HealthData {
    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 采集设备编号 */
    private String deviceId;
    /** 健康综合评分（0-100） */
    private Integer healthScore;
    /** 心率（次/分） */
    private Double heartRate;
    /** 呼吸频率（次/分） */
    private Double breathingRate;
    /** 睡眠状态（深睡/浅睡/清醒） */
    private String sleepStatus;
    /** 睡眠评分（0-100） */
    private Double sleepScore;
    /** 体动指数（监测周期内体动次数） */
    private Double motionIndex;
    /** 在床状态（ON_BED 在床 / OFF_BED 离床） */
    private String onBedStatus;
    /** 体态状态 */
    private String postureStatus;
    /** 设备在线状态（1在线 0离线） */
    private Integer deviceOnline;
    /** 数据上报时间（字符串形式） */
    private String reportTime;
    /** 数据记录时间戳（毫秒） */
    private Long recordedAt;

    /** 老人ID（非数据库表字段，仅用于业务参数传递） */
    @TableField(exist = false)
    private Long elderlyId;
    /** 指标类型（非表字段，前端指标行展示用，如 HEART_RATE） */
    @TableField(exist = false)
    private String metricType;
    /** 指标值（非表字段，前端指标行展示用） */
    @TableField(exist = false)
    private String metricValue;
    /** 指标单位（非表字段，前端指标行展示用，如 次/分） */
    @TableField(exist = false)
    private String unit;

    /**
     * 解析并返回记录时间。
     * 优先按"yyyy-MM-dd HH:mm:ss"解析 reportTime，失败后尝试 ISO 本地日期时间格式；
     * 两者均失败时回退为 recordedAt 毫秒时间戳按东八区转换，全部不可用则返回null。
     *
     * @return 解析得到的记录时间，无法解析时返回null
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime getRecordTime() {
        if (this.reportTime != null) {
            try {
                return LocalDateTime.parse(this.reportTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                try {
                    return LocalDateTime.parse(this.reportTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e2) {
                    if (this.recordedAt != null) {
                        return LocalDateTime.ofEpochSecond(this.recordedAt / 1000, 0, java.time.ZoneOffset.of("+8"));
                    }
                }
            }
        }
        return null;
    }

    /**
     * 按心率、呼吸、睡眠评分、健康综合评分、体动指数的优先级返回第一个非空数值指标。
     *
     * @return 首个非空指标对应的 BigDecimal 值，全部为空时返回null
     */
    public java.math.BigDecimal getNumericValue() {
        if (this.heartRate != null) {
            return java.math.BigDecimal.valueOf(this.heartRate);
        }
        if (this.breathingRate != null) {
            return java.math.BigDecimal.valueOf(this.breathingRate);
        }
        if (this.sleepScore != null) {
            return java.math.BigDecimal.valueOf(this.sleepScore);
        }
        if (this.healthScore != null) {
            return java.math.BigDecimal.valueOf(this.healthScore);
        }
        if (this.motionIndex != null) {
            return java.math.BigDecimal.valueOf(this.motionIndex);
        }
        return null;
    }

    /**
     * 根据当前指标类型 metricType 将数值写入对应指标字段：
     * HEART_RATE 写入心率，BLOOD_OXYGEN 写入健康综合评分，STEPS 写入体动指数；入参为null时不处理。
     *
     * @param numericValue 待写入的指标数值
     */
    public void setNumericValue(java.math.BigDecimal numericValue) {
        if (numericValue != null) {
            if ("HEART_RATE".equals(this.getMetricType())) {
                this.heartRate = numericValue.doubleValue();
            } else if ("BLOOD_OXYGEN".equals(this.getMetricType())) {
                this.healthScore = numericValue.intValue();
            } else if ("STEPS".equals(this.getMetricType())) {
                this.motionIndex = numericValue.doubleValue();
            }
        }
    }

    /**
     * 设置记录时间，并将其按"yyyy-MM-dd HH:mm:ss"格式写入 reportTime 字符串字段；入参为null时不处理。
     *
     * @param recordTime 记录时间
     */
    public void setRecordTime(LocalDateTime recordTime) {
        if (recordTime != null) {
            this.reportTime = recordTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}