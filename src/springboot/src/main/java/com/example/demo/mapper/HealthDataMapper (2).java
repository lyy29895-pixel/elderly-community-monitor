package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.HealthData;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康数据 Mapper 接口
 * 继承 MyBatis-Plus 的 BaseMapper 获得健康记录的基础 CRUD 能力，并扩展按时间范围查询的自定义方法。
 *
 * @author 丙
 */
public interface HealthDataMapper extends BaseMapper<HealthData> {

    /**
     * 按老人ID与时间范围查询健康历史记录，结果按记录时间倒序排列。
     *
     * @param elderlyId 老人ID
     * @param startTime 查询起始时间
     * @param endTime   查询截止时间
     * @return 命中的健康记录列表
     */
    @Select("SELECT * FROM health_history WHERE elderly_id = #{elderlyId} " +
            "AND record_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY record_time DESC")
    List<HealthData> selectByElderlyIdAndTimeRange(
            @Param("elderlyId") Long elderlyId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
