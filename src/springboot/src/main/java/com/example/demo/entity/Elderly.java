package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 老人档案实体，对应数据库 elder_profiles 表，记录老人基本信息、护理与风险评级等。
 * */
@Data
@TableName("elder_profiles")
public class Elderly {
    /** 档案 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 老人对应的登录用户 ID */
    private Long elderUserId;
    /** 老人对应的登录用户名 */
    private String elderUsername;

    /** 老人姓名（非表字段，联表查询时回填） */
    @TableField(exist = false)
    private String name;

    /** 老人真实姓名 */
    private String realName;
    /** 性别 */
    private String gender;
    /** 年龄 */
    private Integer age;
    /** 房间号 */
    private String room;
    /** 家庭住址 */
    private String address;
    /** 老人联系电话 */
    private String phone;

    /** 紧急联系人姓名 */
    private String emergencyContact;
    /** 紧急联系人电话 */
    private String emergencyPhone;

    /** 绑定的监护设备 ID */
    private String monitorDeviceId;
    /** 档案状态 */
    private String status;
    /** 备注信息 */
    private String notes;

    // 护理等级：LEVEL_1(一级/特别护理)、LEVEL_2(二级/重点护理)、LEVEL_3(三级/一般护理)、LEVEL_4(四级/自理)
    /** 护理等级（LEVEL_1 至 LEVEL_4） */
    private String nursingLevel;

    // 风险评级：HIGH(高危)、MEDIUM(中危)、LOW(低危)
    /** 风险评级（HIGH/MEDIUM/LOW） */
    private String riskLevel;

    // 风险因素描述
    /** 风险因素描述（非表字段，风险评估时回填） */
    @TableField(exist = false)
    private String riskFactors;

    // 风险评级时间
    /** 最近一次风险评级时间（非表字段，风险评估时回填） */
    @TableField(exist = false)
    private LocalDateTime riskEvaluatedAt;

    // 健康评分（0-100）
    /** 健康评分，取值 0-100 */
    private Integer healthScore;

    // 是否重病卧床
    /** 是否重病卧床（非表字段，风险评估时回填） */
    @TableField(exist = false)
    private Boolean bedridden;

    // 是否需要高监护优先级
    /** 是否需要高监护优先级（非表字段，风险评估时回填） */
    @TableField(exist = false)
    private Boolean highPriority;

    /** 档案创建时间 */
    private LocalDateTime createdAt;
    /** 档案最近更新时间 */
    private LocalDateTime updatedAt;
}
