package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统登录用户实体，对应数据库 users 表；全平台各端共用，以 role 区分角色。
 * */
@Getter
@Setter
@TableName("users")
public class User {
    /** 用户 ID（主键，自增） */
    @TableId(value = "id", type = IdType.AUTO)
    @JsonProperty("user_id")
    @JsonAlias("id")
    private Integer userId;

    /** 登录用户名 */
    private String username;

    /** 登录密码（明文，仅用于服务端校验，不随响应序列化输出） */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String password;

    /**
     * 获取密码；标注 {@link JsonIgnore}，不会序列化到前端。
     *
     * @return 密码明文
     */
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码；允许请求体中的 password 字段反序列化注入。
     *
     * @param password 密码明文
     */
    @JsonProperty("password")
    public void setPassword(String password) {
        this.password = password;
    }

    /** 用户昵称 */
    private String nickname;
    /** 角色标识（elder/child/community/admin） */
    private String role;
    /** 账号创建时间 */
    private LocalDateTime createdAt;

    /** 年龄（非表字段，联表查询时回填） */
    @TableField(exist = false)
    private Integer age;
    /** 性别（非表字段，联表查询时回填） */
    @TableField(exist = false)
    private String sex;
    /** 住址（非表字段，联表查询时回填） */
    @TableField(exist = false)
    private String address;
    /** 联系电话（非表字段，联表查询时回填） */
    @TableField(exist = false)
    private String phone;
    /** 身份证号（非表字段，联表查询时回填） */
    @TableField(exist = false)
    private String idCard;
    /** 所属社区 ID（非表字段，联表查询时回填） */
    @TableField(exist = false)
    private Long communityId;
    /** 关联老人档案 ID（非表字段，联表查询时回填） */
    @TableField(exist = false)
    private Long elderlyId;
}
