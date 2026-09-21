package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.User;



/**
 * 用户表 Mapper：继承 MyBatis-Plus {@link BaseMapper}，
 * 直接获得 users 表的增删改查能力，无需自定义 SQL。
 *
 * @author 甲
 */
public interface UserMapper extends BaseMapper<User> {
}
