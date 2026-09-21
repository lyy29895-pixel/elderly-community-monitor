package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.EnvironmentData;

/**
 * 环境数据 Mapper 接口
 * 继承 MyBatis-Plus 的 BaseMapper，直接获得环境温湿度记录的基础 CRUD 能力，无需自定义 SQL。
 *
 * @author 乙
 */
public interface EnvironmentDataMapper extends BaseMapper<EnvironmentData> {
}
