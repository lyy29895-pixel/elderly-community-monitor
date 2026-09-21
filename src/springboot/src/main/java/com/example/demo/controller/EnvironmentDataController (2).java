package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.EnvironmentData;
import com.example.demo.service.EnvironmentDataService;
import com.example.demo.util.DateTimeParseUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 环境数据控制器
 * 对外提供指定老人居住环境温湿度历史数据的分页查询接口，路径前缀为 /api/elderly/{elderlyId}/environment。
 *
 * @author 丙
 */
@RestController
@RequestMapping("/api/elderly/{elderlyId}/environment")
public class EnvironmentDataController {

    @Resource
    private EnvironmentDataService environmentDataService;

    /**
     * 分页查询指定老人的环境温湿度历史数据。
     * 起止时间字符串经 {@link DateTimeParseUtil} 灵活解析，缺省时由服务层决定默认时间范围。
     *
     * @param elderlyId 路径参数：老人ID
     * @param pageNum   页码，默认1
     * @param pageSize  每页条数，默认10
     * @param start     起始时间字符串（可空，兼容空格分隔或带T的ISO格式）
     * @param end       截止时间字符串（可空，兼容空格分隔或带T的ISO格式）
     * @return 统一响应体包装的环境数据分页结果
     */
    @GetMapping("/page")
    public Result<Page<EnvironmentData>> page(
            @PathVariable Long elderlyId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        LocalDateTime s = DateTimeParseUtil.parseLocalDateTimeFlexible(start);
        LocalDateTime e = DateTimeParseUtil.parseLocalDateTimeFlexible(end);
        return Result.success(environmentDataService.page(elderlyId, pageNum, pageSize, s, e));
    }
}
