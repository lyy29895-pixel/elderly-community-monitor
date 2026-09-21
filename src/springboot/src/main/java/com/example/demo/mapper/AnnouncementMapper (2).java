package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Announcement;

/**
 * 公告表 Mapper：继承 MyBatis-Plus {@link BaseMapper}，
 * 直接获得 announcements 表的增删改查与分页能力，无需自定义 SQL。
 *
 * @author 乙
 */
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
