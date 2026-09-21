package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.AlertEvent;

/**
 * 告警事件 Mapper 接口。
 * <p>继承 MyBatis-Plus {@link BaseMapper}，自动获得 alerts 表的单表增删改查能力，
 * 复杂筛选由 Service 层结合 LambdaQueryWrapper 完成。</p>
 * */
public interface AlertEventMapper extends BaseMapper<AlertEvent> {
}
