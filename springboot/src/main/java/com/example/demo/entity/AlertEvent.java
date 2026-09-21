package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.demo.service.AlertEventService;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 异常告警事件实体，对应数据库 alerts 表。
 * <p>表内以类型字符串、时间字符串等宽松字段存储历史数据；严重等级、事件发生时间、中文标题、
 * 确认/关闭时间等展示字段不入库，由 getter 按事件类型归类与时间解析动态推导。</p>
 *
 * @author 丙
 */
@Data
@TableName("alerts")
public class AlertEvent {
    /** 事件主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 老人姓名 */
    private String elderName;
    /** 房间号 */
    private String room;
    /** 事件原始类型（如 FALL/SMOKE/心率异常等中英文编码） */
    private String type;
    /** 事件描述 */
    private String description;
    /** 事件发生时间字符串（兼容 yyyy-MM-dd HH:mm:ss 与 ISO 日期时间格式） */
    private String time;
    /** 处理状态（未处理/处理中/已解决/误报等） */
    private String status;
    /** 处置时间线（JSON 字符串） */
    private String timelineJson;
    /** 处理备注 */
    private String notes;
    /** 记录创建时间 */
    private LocalDateTime createdAt;

    /** 关联老人ID（非表字段，查询时按老人姓名匹配档案兜底回填） */
    @TableField(exist = false)
    private Long elderlyId;
    /** 严重等级 1提示/2一般/3紧急（非表字段，为空时按事件类型推导） */
    @TableField(exist = false)
    private Integer severity;
    /** 事件中文标题（非表字段） */
    @TableField(exist = false)
    private String title;
    /** 事件详情（非表字段，为空时取 description） */
    @TableField(exist = false)
    private String detail;
    /** 事件发生时间（非表字段，为空时由 time 解析） */
    @TableField(exist = false)
    private LocalDateTime occurredAt;
    /** 事件确认时间（非表字段） */
    @TableField(exist = false)
    private LocalDateTime acknowledgedAt;
    /** 事件关闭时间（非表字段） */
    @TableField(exist = false)
    private LocalDateTime closedAt;
    /** 处理人用户ID（非表字段） */
    @TableField(exist = false)
    private Integer handlerUserId;
    /** 记录更新时间（非表字段） */
    @TableField(exist = false)
    private LocalDateTime updatedAt;

    /**
     * 获取处理备注，直接复用 notes 字段。
     *
     * @return 处理备注
     */
    public String getRemark() {
        return this.notes;
    }

    /**
     * 设置处理备注，写入 notes 字段。
     *
     * @param remark 处理备注
     */
    public void setRemark(String remark) {
        this.notes = remark;
    }

    /**
     * 获取事件发生时间：优先取 occurredAt；否则依次尝试按常用格式解析 time 字符串，
     * 解析失败再回退到记录创建时间 createdAt。
     *
     * @return 事件发生时间
     */
    public LocalDateTime getOccurredAt() {
        if (this.occurredAt != null) {
            return this.occurredAt;
        }
        if (this.time != null && !this.time.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(this.time, formatter);
            } catch (Exception e) {
                try {
                    return LocalDateTime.parse(this.time, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception ex) {
                    return this.createdAt;
                }
            }
        }
        return this.createdAt;
    }

    /**
     * 获取事件中文标题：显式标题非空时做"摔倒告警→摔倒报警"措辞修正，否则按事件类型实时解析。
     *
     * @return 事件中文标题
     */
    public String getTitle() {
        if (this.title != null && !this.title.trim().isEmpty()) {
            return this.title.replace("摔倒告警", "摔倒报警");
        }
        return AlertEventService.resolveEventTitle(this.type);
    }

    /**
     * 获取事件详情：detail 为空时回退为事件描述。
     *
     * @return 事件详情
     */
    public String getDetail() {
        return this.detail != null ? this.detail : this.description;
    }

    /**
     * 获取事件原始类型，与前端 eventType 字段对齐。
     *
     * @return 事件类型字符串
     */
    public String getEventType() {
        return this.type;
    }

    /**
     * 获取严重等级：显式赋值优先；否则按事件大类推导——摔倒/一键求助/烟雾为 3 级，
     * 情绪低落为 2 级，其余为 1 级。
     *
     * @return 严重等级（1~3）
     */
    public Integer getSeverity() {
        if (this.severity != null) {
            return this.severity;
        }
        String cat = AlertEventService.resolveEventTypeCategory(this.type);
        if ("FALL".equals(cat) || "EMERGENCY".equals(cat) || "SMOKE".equals(cat)) return 3;
        if ("PRESSURE".equals(cat)) return 2;
        return 1;
    }
}
