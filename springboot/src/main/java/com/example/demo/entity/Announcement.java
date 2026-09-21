package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告实体，对应数据库 announcements 表，支持即时发布、预约发布与到期自动撤下。
 *
 * @author 甲
 */
@Data
@TableName("announcements")
public class Announcement {
    /** 公告 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公告标题 */
    private String title;
    /** 公告正文内容 */
    private String content;
    /** 优先级（数值越大优先级越高） */
    private Integer priority;
    
    /** 发布方式：0=即时发布，1=预约发布 */
    private Integer publishType;
    
    /** 实际发布时间 */
    private LocalDateTime publishedAt;
    
    /** 预约发布时间 */
    private LocalDateTime scheduledAt;
    
    /** 结束时间（到期自动撤下） */
    private LocalDateTime endTime;
    
    /** 状态：0=待发布，1=已发布，2=已撤下，3=已过期 */
    private Integer status;
    
    /** 记录创建时间 */
    private LocalDateTime createdAt;
}
