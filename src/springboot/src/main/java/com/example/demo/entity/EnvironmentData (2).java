package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 环境数据实体类
 * 对应数据库表 environment_history，承载老人居住环境的温度、湿度、空气质量、光照度等监测指标。
 *
 * @author 丙
 */
@Data
@TableName("environment_history")
public class EnvironmentData {
    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 采集设备编号 */
    private String deviceId;
    /** 环境温度（℃） */
    private BigDecimal temperature;
    /** 环境相对湿度（%） */
    private BigDecimal humidity;
    /** 空气质量指标 */
    private BigDecimal airQuality;
    /** 光照度 */
    private BigDecimal illumination;
    /** 设备在线状态（1在线 0离线） */
    private Integer deviceOnline;

    /** 数据上报时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportTime;
    /** 数据记录时间戳（毫秒） */
    private Long recordedAt;

    /** 老人ID（非数据库表字段，仅用于业务参数传递） */
    @TableField(exist = false)
    private Long elderlyId;

    /**
     * 获取记录时间（直接返回上报时间字段，序列化为GMT+8时区的ISO格式）。
     *
     * @return 上报时间，未设置时为null
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    public LocalDateTime getRecordTime() {
        return this.reportTime;
    }

    /**
     * 设置记录时间，直接写入上报时间字段。
     *
     * @param recordTime 记录时间
     */
    public void setRecordTime(LocalDateTime recordTime) {
        this.reportTime = recordTime;
    }
}
